package kr.ssok.ssom.backend.domain.issue.service;

import kr.ssok.ssom.backend.domain.alert.dto.AlertIssueRequestDto;
import kr.ssok.ssom.backend.domain.issue.dto.*;
import kr.ssok.ssom.backend.global.dto.LlmIssueResponseDto;

import java.util.List;

/**
 * Issue 관련 서비스 인터페이스
 */
public interface IssueService {
    
    /**
     * LLM을 통한 Issue 초안 작성
     * @param request Issue 생성 요청 DTO
     * @param employeeId 요청자 사원번호
     * @return LLM이 작성한 Issue 초안
     */
    LlmIssueResponseDto createIssueDraft(IssueCreateRequestDto request, String employeeId);
    
    /**
     * GitHub Issue 생성
     * @param request GitHub Issue 생성 요청 DTO
     * @param employeeId 요청자 사원번호
     * @return 생성된 Issue 정보
     */
    IssueResponseDto createGitHubIssue(GitHubIssueCreateRequestDto request, String employeeId);
    
    /**
     * 내가 담당자로 지정된 Issue 목록 조회
     * @param employeeId 요청자 사원번호
     * @return 내가 담당자로 지정된 Issue 목록
     */
    List<IssueResponseDto> getMyIssues(String employeeId);
    
    /**
     * 전체 Issue 목록 조회 (팀 공유)
     * @param employeeId 요청자 사원번호 (로깅용)
     * @return 전체 Issue 목록
     */
    List<IssueResponseDto> getAllIssues(String employeeId);
    
    /**
     * Issue 상세 정보 조회
     * @param issueId Issue ID
     * @param employeeId 요청자 사원번호
     * @return Issue 상세 정보
     */
    IssueResponseDto getIssue(Long issueId, String employeeId);

    /**
     * GitHub Issue Status 변경
     * @param requestDto GitHub Issue payload
     */
    void updateGitHubIssueStatus(AlertIssueRequestDto requestDto);
}
