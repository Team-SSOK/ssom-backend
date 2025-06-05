package kr.ssok.ssom.backend.domain.alert.dto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "AlertIssueRequestDto", description = "이슈 생성 완료 시 보내주는 포맷")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertIssueRequestDto {
    // TODO ISSUE 담당자 분과 협의하여 수정
    private List<String> sharedEmployeeIds; // 이슈 공유자들
    private LocalDateTime timestamp; //이슈 생성 완료 시간
}
