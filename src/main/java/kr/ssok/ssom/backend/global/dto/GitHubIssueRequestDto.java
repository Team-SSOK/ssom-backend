package kr.ssok.ssom.backend.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GitHub Issue 생성 요청 DTO (GitHub API용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GitHub Issue 생성 요청")
public class GitHubIssueRequestDto {
    
    @Schema(description = "Issue 제목", example = "hotfix: Authorization 헤더 누락 시 인증 오류 발생")
    private String title;
    
    @Schema(description = "Issue 본문", example = "## 문제 설명\n운영 환경에서 Authorization 헤더가 없거나...")
    private String body;
    
    @Schema(description = "담당자 GitHub ID 목록", example = "[\"github_user1\", \"github_user2\"]")
    private List<String> assignees;
    
    @Schema(description = "라벨 목록", example = "[\"bug\"]")
    private List<String> labels;
}
