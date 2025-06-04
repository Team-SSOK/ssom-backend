package kr.ssok.ssom.backend.domain.issue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.ssok.ssom.backend.domain.issue.entity.Issue;
import kr.ssok.ssom.backend.domain.issue.entity.constant.IssueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Issue 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Issue 정보 응답")
public class IssueResponseDto {
    
    @Schema(description = "Issue ID", example = "1")
    private Long issueId;
    
    @Schema(description = "GitHub Issue 번호", example = "123")
    private Long githubIssueNumber;
    
    @Schema(description = "Issue 제목", example = "hotfix: Authorization 헤더 누락 시 인증 오류 발생")
    private String title;
    
    @Schema(description = "Issue 설명", example = "운영 환경에서 Authorization 헤더가 없거나...")
    private String description;
    
    @Schema(description = "Issue 상태", example = "OPEN")
    private IssueStatus status;
    
    @Schema(description = "생성자 사원번호", example = "APP0001")
    private String createdByEmployeeId;
    
    @Schema(description = "담당자 GitHub ID 목록", example = "[\"github_user1\", \"github_user2\"]")
    private List<String> assigneeGithubIds;
    
    @Schema(description = "로그 ID 목록", example = "[\"log_001\", \"log_002\"]")
    private List<String> logIds;
    
    @Schema(description = "GitHub 연동 여부", example = "true")
    private Boolean isGithubSynced;
    
    @Schema(description = "생성 일시", example = "2025-05-30T10:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정 일시", example = "2025-05-30T10:30:00")
    private LocalDateTime updatedAt;
    
    /**
     * Issue 엔티티로부터 DTO 생성
     * @param issue Issue 엔티티
     * @return IssueResponseDto
     */
    public static IssueResponseDto from(Issue issue) {
        return IssueResponseDto.builder()
                .issueId(issue.getIssueId())
                .githubIssueNumber(issue.getGithubIssueNumber())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .status(issue.getStatus())
                .createdByEmployeeId(issue.getCreatedByEmployeeId())
                .assigneeGithubIds(issue.getAssigneeGithubIds())
                .logIds(issue.getLogIds())
                .isGithubSynced(issue.isGithubSynced())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }
}
