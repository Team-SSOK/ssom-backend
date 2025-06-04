package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Getter;

@Getter
public class AlertSendRequestDto {
    private String level;
    private String app;
    private String timestamp;
    private String message;
}
