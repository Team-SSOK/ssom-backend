package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AlertOpensearchRequestDto {
    private String app;
    private String level;
    private String message;
    private LocalDateTime timestamp;
}
