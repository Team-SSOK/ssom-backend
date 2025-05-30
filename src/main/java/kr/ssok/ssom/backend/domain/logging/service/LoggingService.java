package kr.ssok.ssom.backend.domain.logging.service;

import kr.ssok.ssom.backend.domain.issue.dto.LogDataDto;
import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.global.dto.LogRequestDto;
import kr.ssok.ssom.backend.global.dto.LogSummaryMessageDto;

import java.util.List;

public interface LoggingService {

    ServicesResponseDto getServices();

    LogsResponseDto getLogs();

    LogSummaryMessageDto getLogInfo(String logId);

    LogSummaryMessageDto summarizeLog(LogDto request);
    
    /**
     * 로그 ID 목록으로 로그 데이터 조회 (Issue 생성용)
     * @param logIds 로그 ID 목록
     * @return 로그 데이터 목록
     */
    List<LogDataDto> getLogsByIds(List<String> logIds);
    
    /**
     * 로그 데이터를 LLM API 요청 형식으로 변환
     * @param logDataList 로그 데이터 목록
     * @return LLM API 요청용 로그 목록
     */
    List<LogRequestDto> convertToLlmRequestFormat(List<LogDataDto> logDataList);
}
