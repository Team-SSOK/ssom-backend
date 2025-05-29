package kr.ssok.ssom.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSummaryRequestDto {
    private String level;
    private String logger;
    private String thread;
    private String message;
    private String app;
}
