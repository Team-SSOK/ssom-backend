package kr.ssok.ssom.backend.domain.logging.service;

import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.global.dto.LogSummaryRequestDto;
import kr.ssok.ssom.backend.global.dto.LogSummaryResponseDto;

public interface LoggingService {

    ServicesResponseDto getServices();

    LogsResponseDto getLogs();

    LogInfoResponseDto getLogInfo(String logId);

    LogSummaryResponseDto summarizeLog(LogSummaryRequestDto request);
}
