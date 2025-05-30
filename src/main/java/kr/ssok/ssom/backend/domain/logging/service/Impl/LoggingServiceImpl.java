package kr.ssok.ssom.backend.domain.logging.service.Impl;

import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.issue.dto.LogDataDto;
import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.domain.logging.entity.LogSummary;
import kr.ssok.ssom.backend.domain.logging.entity.Logging;
import kr.ssok.ssom.backend.domain.logging.repository.LogSummaryRepository;
import kr.ssok.ssom.backend.domain.logging.repository.LoggingRepository;
import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.global.client.LlmServiceClient;
import kr.ssok.ssom.backend.global.dto.*;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingServiceImpl implements LoggingService {

    private final LogSummaryRepository logSummaryRepository;
    private final LoggingRepository loggingRepository;
    private final LlmServiceClient llmServiceClient;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 1시간
    private static final String SSE_EVENT_TYPE = "logging";

    /**
     * 서비스 목록 조회
     */
    @Override
    public ServicesResponseDto getServices() {
        // OpenSearch에 요청 보내기?
        return null;
    }

    /**
     * 로그 목록 조회
     */
    @Override
    public LogsResponseDto getLogs() {
        // 초기: 모든 서비스, 모든 레벨

        // 필터링 시: 조건에 맞는 서비스와 레벨만 조회
        return null;
    }

    /**
     * 로그 SSE 구독
     */
    public SseEmitter subscribe(String employeeId, String lastEventId, HttpServletResponse response){
        log.info("[로그 SSE 구독] 서비스 진입");

        String emitterId = createTimeIncludeId(employeeId);

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(emitterId, emitter);

        response.setHeader("X-Accel-Buffering", "no");

        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));
        emitter.onError((e) -> emitters.remove(emitterId));

        try {
            String eventId = createTimeIncludeId(employeeId);
            emitter.send(SseEmitter.event().id(eventId).name("INIT").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitterId);
            emitter.completeWithError(e);
            throw new RuntimeException("sse send failed" + e);
        }

        log.info("sse 연결 완료");

        return emitter;
    }

    private String createTimeIncludeId(String employeeId) {
        return employeeId + "_" + System.currentTimeMillis() + "_" + SSE_EVENT_TYPE;
    }

    /**
     * 로그 SSE 전송 (실시간으로 뜨는 로그를 하나씩)
     */
    public void sendLogToUser(String emitterId, LogDto logDto) {
        log.info("[로그 SSE 전송] 서비스 진입");

        SseEmitter emitter = emitters.get(emitterId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("logging")
                        .data(logDto));
            } catch (IOException e) {
                emitters.remove(emitterId);
            }
        }
    }

    /**
     * 로그 상세 조회 - 이전에 생성한 LLM 요약 반환
     */
    @Override
    public LogSummaryMessageDto getLogInfo(String logId) {

        // DB에 로그 아이디로 조회
        Optional<LogSummary> summaryOpt = logSummaryRepository.findByLogId(logId);

        // 없을 시 아무것도 반환하지 않음
        if (summaryOpt.isEmpty()) {
            return null;
        }

        // 있을 시 DB에서 조회된 기존 요약 내용 반환
        LogSummary summaryEntity = summaryOpt.get();

        LogLocationDto locationDto = LogLocationDto.builder()
                .file(summaryEntity.getFileLocation())
                .function(summaryEntity.getFunctionLocation())
                .build();
        LogSummaryMessageDto summaryDto = LogSummaryMessageDto.builder()
                .title(summaryEntity.getTitle())
                .location(locationDto)
                .solution(summaryEntity.getSolution())
                .build();

        return summaryDto;
    }

    /**
     * 로그 상세 조회 - 새롭게 생성한 LLM 요약 반환
     */
    @Override
    public LogSummaryMessageDto summarizeLog(LogDto request) {

        // LLM 쪽으로 api 요청
        LogRequestDto requestDto = LogRequestDto.builder()
                .level(request.getLevel())
                .logger(request.getLogger())
                .thread(request.getThread())
                .message(request.getMessage())
                .app(request.getApp())
                .build();
        LlmApiRequestDto llmRequestDto = LlmApiRequestDto.builder()
                .logs(List.of(requestDto))
                .build();
        LlmApiResponseDto<LogSummaryResponseDto> llmResponseDto = llmServiceClient.summarizeLog(llmRequestDto);

        // 응답에서 요약 메시지 추출
        LogSummaryMessageDto summaryDto = llmResponseDto.getResult().get(0).getMessage();

        // DB에 저장
        LogSummary summaryEntity = LogSummary.builder()
                .logId(request.getLogId())
                .title(summaryDto.getTitle())
                .fileLocation(summaryDto.getLocation().getFile())
                .functionLocation(summaryDto.getLocation().getFunction())
                .solution(summaryDto.getSolution())
                .build();
        logSummaryRepository.save(summaryEntity);

        // 분석 내용 반환
        return summaryDto;
    }

    /**
     * 로그 ID 목록으로 로그 데이터 조회 (Issue 생성용)
     */
    @Override
    public List<LogDataDto> getLogsByIds(List<String> logIds) {
        log.info("로그 ID 목록으로 로그 조회: {}", logIds);

        // DB에서 로그 조회
        List<Logging> logs = loggingRepository.findByLogIds(logIds);

        // 조회되지 않은 로그 ID 확인
        List<String> foundLogIds = logs.stream()
                .map(Logging::getLogId)
                .collect(Collectors.toList());

        List<String> notFoundLogIds = logIds.stream()
                .filter(logId -> !foundLogIds.contains(logId))
                .collect(Collectors.toList());

        if (!notFoundLogIds.isEmpty()) {
            log.warn("조회되지 않은 로그 ID들: {}", notFoundLogIds);
            throw new BaseException(BaseResponseStatus.NOT_FOUND_LOG);
        }

        // Logging Entity를 LogDataDto로 변환
        return logs.stream()
                .map(this::convertToLogDataDto)
                .collect(Collectors.toList());
    }

    /**
     * 로그 데이터를 LLM API 요청 형식으로 변환
     */
    @Override
    public List<LogRequestDto> convertToLlmRequestFormat(List<LogDataDto> logDataList) {
        return logDataList.stream()
                .map(logData -> LogRequestDto.builder()
                        .level(logData.getLevel())
                        .logger(logData.getLogger())
                        .thread(logData.getThread())
                        .message(logData.getMessage())
                        .app(logData.getApp())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Logging Entity를 LogDataDto로 변환
     */
    private LogDataDto convertToLogDataDto(Logging logging) {
        return LogDataDto.builder()
                .logId(logging.getLogId())
                .level(logging.getLevel())
                .logger(logging.getLogger())
                .thread(logging.getThread())
                .message(logging.getMessage())
                .app(logging.getApp())
                .timestamp(logging.getTimestamp())
                .build();
    }
}
