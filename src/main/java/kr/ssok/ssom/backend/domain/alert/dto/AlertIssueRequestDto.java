package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AlertIssueRequestDto {
    private String app;
    private String level;
    private String message;
    private LocalDateTime timestamp;
}
