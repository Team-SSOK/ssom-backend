package kr.ssok.ssom.backend.domain.issue.service.Impl;

import kr.ssok.ssom.backend.domain.issue.dto.*;
import kr.ssok.ssom.backend.domain.issue.entity.Issue;
import kr.ssok.ssom.backend.domain.issue.entity.constant.IssueStatus;
import kr.ssok.ssom.backend.domain.issue.repository.IssueRepository;
import kr.ssok.ssom.backend.domain.issue.service.IssueService;
import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.global.client.LlmServiceClient;
import kr.ssok.ssom.backend.global.dto.*;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * GitHub Issue 생성 (Phase 3에서 구현 예정)
     */
    @Override
    @Transactional
    public IssueResponseDto createGitHubIssue(GitHubIssueCreateRequestDto request, String employeeId) {
        log.info("GitHub Issue 생성 요청 - 사원번호: {}, 제목: {}", employeeId, request.getTitle());
        
        // TODO: Phase 3에서 GitHub API 연동 구현
        // 현재는 DB에만 저장하는 로직으로 임시 구현
        
        Issue issue = Issue.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(IssueStatus.OPEN)
                .createdByEmployeeId(employeeId)
                .logIds(request.getLogIds())
                .githubIssueNumber(null) // GitHub 연동 전이므로 null
                .build();
        
        Issue savedIssue = issueRepository.save(issue);
        log.info("Issue DB 저장 완료 - Issue ID: {}", savedIssue.getIssueId());
        
        return IssueResponseDto.from(savedIssue);
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
     * GitHub Webhook을 통한 Issue 상태 동기화 (Phase 6에서 구현 예정)
     */
    @Override
    @Transactional
    public void syncIssueStatus(Long githubIssueNumber, String newStatus) {
        log.info("GitHub Issue 상태 동기화 - GitHub Issue 번호: {}, 새로운 상태: {}", githubIssueNumber, newStatus);
        
        // TODO: Phase 6에서 GitHub Webhook 연동 구현
        
        Issue issue = issueRepository.findByGithubIssueNumber(githubIssueNumber)
                .orElse(null);
        
        if (issue == null) {
            log.warn("GitHub Issue 번호에 해당하는 Issue를 찾을 수 없음: {}", githubIssueNumber);
            return;
        }
        
        // 상태 변환 및 업데이트
        IssueStatus issueStatus;
        try {
            issueStatus = IssueStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 Issue 상태: {}", newStatus);
            return;
        }
        
        issue.updateStatus(issueStatus);
        issueRepository.save(issue);
        
        log.info("Issue 상태 동기화 완료 - Issue ID: {}, 새로운 상태: {}", issue.getIssueId(), issueStatus);
    }
}
