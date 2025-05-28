package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Getter;

@Getter
public class AlertModifyRequestDto {
    private Long alertStatusId;
    private boolean isRead;
}

