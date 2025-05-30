package kr.ssok.ssom.backend.domain.issue.service.Impl;

import kr.ssok.ssom.backend.domain.issue.dto.*;
import kr.ssok.ssom.backend.domain.issue.entity.Issue;
import kr.ssok.ssom.backend.domain.issue.entity.constant.IssueStatus;
import kr.ssok.ssom.backend.domain.issue.repository.IssueRepository;
import kr.ssok.ssom.backend.domain.issue.service.IssueService;
import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.repository.UserRepository;
import kr.ssok.ssom.backend.global.client.GitHubApiClient;
import kr.ssok.ssom.backend.global.client.LlmServiceClient;
import kr.ssok.ssom.backend.global.config.GitHubConfig;
import kr.ssok.ssom.backend.global.dto.*;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Issue 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueServiceImpl implements IssueService {
    
    private final IssueRepository issueRepository;
    private final LoggingService loggingService;
    private final LlmServiceClient llmServiceClient;
    private final GitHubApiClient gitHubApiClient;
    private final GitHubConfig gitHubConfig;
    private final UserRepository userRepository;
    
    /**
     * LLM을 통한 Issue 초안 작성
     */
    @Override
    public LlmIssueResponseDto createIssueDraft(IssueCreateRequestDto request, String employeeId) {
        log.info("Issue 초안 작성 요청 - 사원번호: {}, 로그 ID 개수: {}", employeeId, request.getLogIds().size());
        
        try {
            // 1. 로그 ID들로 실제 로그 데이터 조회
            List<LogDataDto> logDataList = loggingService.getLogsByIds(request.getLogIds());
            log.info("조회된 로그 개수: {}", logDataList.size());
            
            // 2. 로그 데이터를 LLM API 요청 형식으로 변환
            List<LogRequestDto> llmRequestLogs = loggingService.convertToLlmRequestFormat(logDataList);
            
            // 3. LLM API 요청 DTO 구성
            LlmApiRequestDto llmRequest = LlmApiRequestDto.builder()
                    .logs(llmRequestLogs)
                    .build();
            
            // 4. LLM API 호출하여 Issue 초안 작성
            log.info("LLM API 호출 시작");
            LlmApiResponseDto<LlmIssueResponseDto> llmResponse = llmServiceClient.writeIssue(llmRequest);
            
            // 5. LLM API 응답 검증
            if (!llmResponse.isSuccess() || llmResponse.getResult() == null || llmResponse.getResult().isEmpty()) {
                log.error("LLM API 응답 실패 - Success: {}, Message: {}", llmResponse.isSuccess(), llmResponse.getMessage());
                throw new BaseException(BaseResponseStatus.LLM_API_ERROR);
            }
            
            LlmIssueResponseDto issueResponse = llmResponse.getResult().get(0);
            log.info("LLM Issue 초안 작성 완료 - 제목: {}", issueResponse.getMessage().getTitle());
            
            return issueResponse;
            
        } catch (BaseException e) {
            log.error("Issue 초안 작성 실패 - BaseException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Issue 초안 작성 중 예상치 못한 오류 발생", e);
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * GitHub Issue 생성
     */
    @Override
    @Transactional
    public IssueResponseDto createGitHubIssue(GitHubIssueCreateRequestDto request, String employeeId) {
        log.info("GitHub Issue 생성 요청 - 사원번호: {}, 제목: {}", employeeId, request.getTitle());
        
        try {
            // 1. 요청자 정보 조회
            User creator = userRepository.findById(employeeId)
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
            
            // 2. 담당자들의 GitHub ID 수집
            List<String> assigneeGithubIds = collectAssigneeGithubIds(request.getAssigneeUsernames(), creator);
            
            // 3. GitHub Issue 본문 생성 (Markdown 형식)
            String issueBody = buildIssueBody(request);
            
            // 4. GitHub API 요청 구성
            GitHubIssueRequestDto githubRequest = GitHubIssueRequestDto.builder()
                    .title(request.getTitle())
                    .body(issueBody)
                    .assignees(assigneeGithubIds)
                    .labels(getDefaultLabels())
                    .build();
            
            // 5. GitHub API 호출 (실패 시 전체 트랜잭션 롤백)
            GitHubIssueResponseDto githubResponse;
            try {
                githubResponse = gitHubApiClient.createIssue(
                        gitHubConfig.getApi().getOwner(),
                        gitHubConfig.getApi().getRepository(),
                        gitHubConfig.getAuthorizationHeader(),
                        githubRequest
                );
                log.info("GitHub Issue 생성 성공 - GitHub Issue 번호: {}, URL: {}", 
                        githubResponse.getNumber(), githubResponse.getHtmlUrl());
                
            } catch (Exception e) {
                log.error("GitHub Issue 생성 실패 - 오류: {}", e.getMessage());
                throw new BaseException(BaseResponseStatus.GITHUB_API_ERROR);
            }
            
            // 6. GitHub 연동 성공 후 DB에 저장
            Issue issue = Issue.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .status(IssueStatus.OPEN)
                    .createdByEmployeeId(employeeId)
                    .assigneeGithubIds(assigneeGithubIds)
                    .logIds(request.getLogIds())
                    .githubIssueNumber(githubResponse.getNumber()) // GitHub 연동 완료
                    .build();
            
            Issue savedIssue = issueRepository.save(issue);
            log.info("Issue DB 저장 완료 - Issue ID: {}, GitHub Issue 번호: {}", 
                    savedIssue.getIssueId(), githubResponse.getNumber());
            
            return IssueResponseDto.from(savedIssue);
            
        } catch (BaseException e) {
            log.error("GitHub Issue 생성 실패 - BaseException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("GitHub Issue 생성 중 예상치 못한 오류 발생", e);
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 사용자가 생성한 Issue 목록 조회
     */
    @Override
    public List<IssueResponseDto> getMyIssues(String employeeId) {
        log.info("사용자 Issue 목록 조회 - 사원번호: {}", employeeId);
        
        List<Issue> issues = issueRepository.findByCreatedByEmployeeIdOrderByCreatedAtDesc(employeeId);
        
        return issues.stream()
                .map(IssueResponseDto::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 전체 Issue 목록 조회 (팀 공유)
     */
    @Override
    public List<IssueResponseDto> getAllIssues(String employeeId) {
        log.info("전체 Issue 목록 조회 - 요청자: {}", employeeId);
        
        List<Issue> issues = issueRepository.findAllByOrderByCreatedAtDesc();
        
        return issues.stream()
                .map(IssueResponseDto::from)
                .collect(Collectors.toList());
    }
    
    /**
     * Issue 상세 정보 조회
     */
    @Override
    public IssueResponseDto getIssue(Long issueId, String employeeId) {
        log.info("Issue 상세 조회 - Issue ID: {}, 요청자: {}", issueId, employeeId);
        
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_ISSUE));
        
        return IssueResponseDto.from(issue);
    }
    
    /**
     * GitHub Webhook을 통한 Issue 상태 동기화 (Phase 6에서 완전 구현 예정)
     */
    @Override
    @Transactional
    public void syncIssueStatus(Long githubIssueNumber, String newStatus) {
        log.info("GitHub Issue 상태 동기화 - GitHub Issue 번호: {}, 새로운 상태: {}", githubIssueNumber, newStatus);
        
        Issue issue = issueRepository.findByGithubIssueNumber(githubIssueNumber)
                .orElse(null);
        
        if (issue == null) {
            log.warn("GitHub Issue 번호에 해당하는 Issue를 찾을 수 없음: {}", githubIssueNumber);
            return;
        }
        
        // 상태 변환 및 업데이트
        IssueStatus issueStatus;
        try {
            issueStatus = "closed".equalsIgnoreCase(newStatus) ? IssueStatus.CLOSED : IssueStatus.OPEN;
        } catch (Exception e) {
            log.warn("알 수 없는 Issue 상태: {}", newStatus);
            return;
        }
        
        issue.updateStatus(issueStatus);
        issueRepository.save(issue);
        
        log.info("Issue 상태 동기화 완료 - Issue ID: {}, 새로운 상태: {}", issue.getIssueId(), issueStatus);
    }
    
    /**
     * 담당자들의 GitHub ID 수집
     */
    private List<String> collectAssigneeGithubIds(List<String> assigneeUsernames, User creator) {
        List<String> githubIds = new ArrayList<>();
        
        if (assigneeUsernames != null && !assigneeUsernames.isEmpty()) {
            // 지정된 담당자들의 GitHub ID 수집
            for (String username : assigneeUsernames) {
                List<User> users = userRepository.findByUsernameContainingIgnoreCaseAndGithubIdIsNotNull(username);
                
                if (!users.isEmpty()) {
                    // 정확히 일치하는 사용자를 우선으로 찾기
                    User exactMatch = users.stream()
                            .filter(user -> user.getUsername().equalsIgnoreCase(username))
                            .findFirst()
                            .orElse(users.get(0)); // 정확한 매치가 없으면 첫 번째 결과 사용
                    
                    if (!githubIds.contains(exactMatch.getGithubId())) {
                        githubIds.add(exactMatch.getGithubId());
                    }
                } else {
                    log.warn("GitHub ID가 있는 사용자를 찾을 수 없음: {}", username);
                }
            }
        }
        
        // 담당자가 지정되지 않았거나 모두 찾을 수 없는 경우 본인을 담당자로 추가
        if (githubIds.isEmpty() && creator.getGithubId() != null && !creator.getGithubId().trim().isEmpty()) {
            githubIds.add(creator.getGithubId());
        }
        
        return githubIds;
    }
    
    /**
     * GitHub Issue 본문 생성 (Markdown 형식)
     */
    private String buildIssueBody(GitHubIssueCreateRequestDto request) {
        StringBuilder body = new StringBuilder();
        
        // 기본 설명
        body.append("## 📝 Issue 설명\n");
        body.append(request.getDescription()).append("\n\n");
        
        // 오류 위치 (LLM이 제공한 경우)
        if (request.getLocationFile() != null || request.getLocationFunction() != null) {
            body.append("## 📍 오류 위치\n");
            if (request.getLocationFile() != null) {
                body.append("**파일:** `").append(request.getLocationFile()).append("`\n");
            }
            if (request.getLocationFunction() != null) {
                body.append("**함수:** `").append(request.getLocationFunction()).append("`\n");
            }
            body.append("\n");
        }
        
        // 원인 분석 (LLM이 제공한 경우)
        if (request.getCause() != null && !request.getCause().trim().isEmpty()) {
            body.append("## 🔍 원인 분석\n");
            body.append(request.getCause()).append("\n\n");
        }
        
        // 재현 단계 (LLM이 제공한 경우)
        if (request.getReproductionSteps() != null && !request.getReproductionSteps().isEmpty()) {
            body.append("## 🔄 재현 단계\n");
            for (int i = 0; i < request.getReproductionSteps().size(); i++) {
                body.append(i + 1).append(". ").append(request.getReproductionSteps().get(i)).append("\n");
            }
            body.append("\n");
        }
        
        // 해결 방안 (LLM이 제공한 경우)
        if (request.getSolution() != null && !request.getSolution().trim().isEmpty()) {
            body.append("## 💡 해결 방안\n");
            body.append(request.getSolution()).append("\n\n");
        }
        
        // 참조 파일들 (LLM이 제공한 경우)
        if (request.getReferences() != null && !request.getReferences().trim().isEmpty()) {
            body.append("## 📚 참조 파일\n");
            body.append(request.getReferences()).append("\n\n");
        }
        
        // 관련 로그 정보
        body.append("## 🔍 관련 로그\n");
        body.append("**로그 ID 목록:** ");
        body.append(String.join(", ", request.getLogIds())).append("\n\n");
        
        // 추가 정보
        body.append("## 📋 추가 정보\n");
        body.append("- **생성자:** SSOM 시스템\n");
        body.append("- **생성 시간:** ").append(java.time.LocalDateTime.now()).append("\n");
        body.append("- **로그 개수:** ").append(request.getLogIds().size()).append("개\n\n");
        
        body.append("---\n");
        body.append("*이 Issue는 SSOM 백엔드 시스템에서 LLM을 통해 자동으로 생성되었습니다.*");
        
        return body.toString();
    }
    
    /**
     * 기본 라벨 목록 반환
     */
    private List<String> getDefaultLabels() {
        return List.of("bug");
    }
}
