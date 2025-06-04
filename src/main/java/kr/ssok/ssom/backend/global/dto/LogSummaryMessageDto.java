package kr.ssok.ssom.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSummaryMessageDto {
    private String summary;
    private LogLocationDto location;
    private String solution;
    @JsonProperty("solution_detail")
    private String solutionDetail;
}
