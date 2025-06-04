package kr.ssok.ssom.backend.domain.logging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogDataDto {
    @JsonProperty("@timestamp")
    private String timestamp;

    private String level;
    private String logger;
    private String thread;
    private String message;
    private String app;
}
