package kr.ssok.ssom.backend.domain.logging.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogDto {
    private String logId;
    private String timestamp;

    private String level;
    private String logger;
    private String thread;
    private String message;
    private String app;
}
