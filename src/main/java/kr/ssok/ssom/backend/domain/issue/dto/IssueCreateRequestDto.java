package kr.ssok.ssom.backend.domain.issue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM Issue 초안 작성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "LLM Issue 초안 작성 요청")
public class IssueCreateRequestDto {
    
    @Schema(description = "로그 ID 목록", example = "[\"log_001\", \"log_002\"]")
    @NotEmpty(message = "로그 ID 목록은 필수입니다.")
    @Size(min = 1, max = 10, message = "로그는 1개 이상 10개 이하로 선택해주세요.")
    private List<String> logIds;
    
    @Schema(description = "추가 컨텍스트 정보 (선택사항)", example = "운영 환경에서 발생한 오류입니다.")
    private String additionalContext;
}
