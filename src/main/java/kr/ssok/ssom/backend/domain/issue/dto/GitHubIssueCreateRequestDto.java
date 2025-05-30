package kr.ssok.ssom.backend.domain.issue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GitHub Issue 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GitHub Issue 생성 요청")
public class GitHubIssueCreateRequestDto {
    
    @Schema(description = "Issue 제목", example = "hotfix: Authorization 헤더 누락 시 인증 오류 발생")
    @NotBlank(message = "Issue 제목은 필수입니다.")
    @Size(max = 255, message = "제목은 255자 이하로 입력해주세요.")
    private String title;
    
    @Schema(description = "Issue 설명", example = "운영 환경에서 Authorization 헤더가 없거나 형식이 잘못된 요청에 대해...")
    @NotBlank(message = "Issue 설명은 필수입니다.")
    private String description;
    
    @Schema(description = "로그 ID 목록", example = "[\"log_001\", \"log_002\"]")
    @NotEmpty(message = "로그 ID 목록은 필수입니다.")
    @Size(min = 1, max = 10, message = "로그는 1개 이상 10개 이하로 선택해주세요.")
    private List<String> logIds;
    
    @Schema(description = "담당자 사용자명 목록 (선택사항)", example = "[\"홍길동\", \"김철수\"]")
    private List<String> assigneeUsernames;
    
    @Schema(description = "라벨 목록 (선택사항)", example = "[\"bug\", \"hotfix\"]")
    private List<String> labels;
}
