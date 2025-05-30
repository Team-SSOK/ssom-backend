package kr.ssok.ssom.backend.global.client;

import kr.ssok.ssom.backend.global.dto.GitHubIssueRequestDto;
import kr.ssok.ssom.backend.global.dto.GitHubIssueResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * GitHub API FeignClient
 */
@FeignClient(name = "github-api", url = "https://api.github.com")
public interface GitHubApiClient {
    
    /**
     * GitHub Issue 생성
     * @param owner Repository 소유자 (조직명 또는 사용자명)
     * @param repo Repository 이름
     * @param authorization GitHub Personal Access Token (Bearer token 형식)
     * @param request Issue 생성 요청 데이터
     * @return 생성된 Issue 정보
     */
    @PostMapping("/repos/{owner}/{repo}/issues")
    GitHubIssueResponseDto createIssue(
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repo,
            @RequestHeader("Authorization") String authorization,
            @RequestBody GitHubIssueRequestDto request
    );
    
    /**
     * GitHub Issue 조회
     * @param owner Repository 소유자
     * @param repo Repository 이름
     * @param issueNumber Issue 번호
     * @param authorization GitHub Personal Access Token
     * @return Issue 정보
     */
    @GetMapping("/repos/{owner}/{repo}/issues/{issue_number}")
    GitHubIssueResponseDto getIssue(
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repo,
            @PathVariable("issue_number") Long issueNumber,
            @RequestHeader("Authorization") String authorization
    );
    
    /**
     * GitHub Issue 상태 변경 (닫기/열기)
     * @param owner Repository 소유자
     * @param repo Repository 이름
     * @param issueNumber Issue 번호
     * @param authorization GitHub Personal Access Token
     * @param request 상태 변경 요청 (state: "open" 또는 "closed")
     * @return 수정된 Issue 정보
     */
    @PatchMapping("/repos/{owner}/{repo}/issues/{issue_number}")
    GitHubIssueResponseDto updateIssue(
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repo,
            @PathVariable("issue_number") Long issueNumber,
            @RequestHeader("Authorization") String authorization,
            @RequestBody GitHubIssueUpdateRequest request
    );
    
    /**
     * GitHub Issue 상태 변경 요청 DTO
     */
    record GitHubIssueUpdateRequest(String state) {}
}
