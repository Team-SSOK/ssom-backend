package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AlertModifyRequestDto {
    private Long alertStatusId;
    private boolean isRead;
}

