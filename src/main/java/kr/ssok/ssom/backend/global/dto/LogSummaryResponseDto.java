package kr.ssok.ssom.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSummaryResponseDto {
    private String title;
    private LogLocationDto location;
    private String solution;
}
