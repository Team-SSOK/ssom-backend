package kr.ssok.ssom.backend.domain.issue.service;

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
     * 사용자가 생성한 Issue 목록 조회
     * @param employeeId 사용자 사원번호
     * @return Issue 목록
     */
    List<IssueResponseDto> getMyIssues(String employeeId);
    
    /**
     * Issue 상세 정보 조회
     * @param issueId Issue ID
     * @param employeeId 요청자 사원번호
     * @return Issue 상세 정보
     */
    IssueResponseDto getIssue(Long issueId, String employeeId);
    
    /**
     * GitHub Webhook을 통한 Issue 상태 동기화
     * @param githubIssueNumber GitHub Issue 번호
     * @param newStatus 새로운 상태
     */
    void syncIssueStatus(Long githubIssueNumber, String newStatus);
}
