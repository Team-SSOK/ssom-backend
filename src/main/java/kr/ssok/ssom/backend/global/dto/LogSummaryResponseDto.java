package kr.ssok.ssom.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSummaryResponseDto {
    private List<LogRequestDto> logs;
    private LogSummaryMessageDto message;
}
