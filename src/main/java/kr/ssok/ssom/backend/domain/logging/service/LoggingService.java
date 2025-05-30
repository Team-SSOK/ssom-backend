package kr.ssok.ssom.backend.domain.logging.service;

import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.global.dto.LogRequestDto;
import kr.ssok.ssom.backend.global.dto.LogSummaryMessageDto;

public interface LoggingService {

    ServicesResponseDto getServices();

    LogsResponseDto getLogs();

    LogSummaryMessageDto getLogInfo(String logId);

    LogSummaryMessageDto summarizeLog(LogDto request);
}
