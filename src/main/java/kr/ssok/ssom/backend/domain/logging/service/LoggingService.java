package kr.ssok.ssom.backend.domain.logging.service;

import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.global.dto.LogRequestDto;
import kr.ssok.ssom.backend.global.dto.LogSummaryMessageDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface LoggingService {

    /**
     * OpenSearch로부터 서비스 목록 조회
     * @return 서비스 목록
     */
    ServicesResponseDto getServices();

    /**
     * OpenSearch로부터 로그 목록 조회
     * @param app 필터링 조건 (서비스)
     * @param level 필터링 조건 (로그 레벨)
     * @return 로그 목록
     */
    LogsResponseDto getLogs(String app, String level);


    /**
     * SSE 구독
     * @param employeeId
     * @param app
     * @param level
     * @param response
     * @return SSE Emitter
     */
    SseEmitter subscribe(String employeeId, String app, String level, HttpServletResponse response);

    /**
     * OpenSearch가 실시간 로그 알림을 줄 때 사용되는 기능
     * @param requestStr
     */
    void createOpensearchAlert(String requestStr);


    /**
     * 특정 로그에 대한 기존 LLM 요약 데이터 조회
     * @param logId 로그 아이디
     * @return DB로부터 조회한 LLM 요약 데이터
     */
    LogSummaryMessageDto getLogAnalysisInfo(String logId);

    /**
     * 특정 로그에 대한 새 LLM 요약 데이터 생성
     * @param request 로그
     * @return LLM으로부터 생성한 요약 데이터
     */
    LogSummaryMessageDto analyzeLog(LogDto request);


    /**
     * 로그 ID로 로그 데이터 조회
     * @param logId 로그 ID
     * @return 로그 데이터
     */
    LogDto getLogById(String logId);

    /**
     * 로그 ID 목록으로 로그 데이터 조회 (Issue 생성용)
     * @param logIds 로그 ID 목록
     * @return 로그 데이터 목록
     */
    List<LogDto> getLogsByIds(List<String> logIds);
    
    /**
     * 로그 데이터를 LLM API 요청 형식으로 변환
     * @param logList 로그 데이터 목록
     * @return LLM API 요청용 로그 목록
     */
    List<LogRequestDto> convertToLlmRequestFormat(List<LogDto> logList);


}
