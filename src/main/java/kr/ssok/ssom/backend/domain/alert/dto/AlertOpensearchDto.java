package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AlertOpensearchDto {
    private String id;
    private String level;
    private String app;
    private LocalDateTime timestamp;
    private String message;
}
