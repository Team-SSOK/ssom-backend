package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AlertIssueRequestDto {
    // TODO ISSUE 담당자 분과 협의하여 수정
    private List<String> sharedEmployeeIds; // 이슈 공유자들
}
