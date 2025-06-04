package kr.ssok.ssom.backend.domain.logging.service.Impl;

import jakarta.servlet.http.HttpServletResponse;

import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.domain.logging.entity.LogSummary;
import kr.ssok.ssom.backend.domain.logging.repository.LogSummaryRepository;
import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.global.client.LlmServiceClient;
import kr.ssok.ssom.backend.global.dto.*;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.MgetRequest;
import org.opensearch.client.opensearch.core.MgetResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
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
    private final LlmServiceClient llmServiceClient;
    private final OpenSearchClient openSearchClient;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 1시간
    private static final String SSE_EVENT_TYPE = "logging";

    /**
     * 서비스 목록 조회
     */
    @Override
    public ServicesResponseDto getServices() throws IOException {

        SearchRequest request = new SearchRequest.Builder()
                .index("ssok-app")
                .size(0)  // hits는 필요 없으므로 0으로
                .aggregations("apps", agg -> agg
                        .terms(t -> t
                                .field("app.keyword")
                                .size(100)
                        )
                )
                .build();

        SearchResponse<Void> response = openSearchClient.search(request, Void.class);

        List<ServiceDto> result = new ArrayList<>();

        // "apps" aggregation 추출
        var appsAgg = response.aggregations().get("apps").sterms();

        for (StringTermsBucket bucket : appsAgg.buckets().array()) {
            result.add(new ServiceDto(bucket.key(), bucket.docCount()));
        }

        return (ServicesResponseDto) result;
    }

    /**
     * 로그 목록 조회
     */
    @Override
    public LogsResponseDto getLogs() throws Exception {
        // 초기: 모든 서비스, 모든 레벨

        // 필터링 시: 조건에 맞는 서비스와 레벨만 조회

        SearchRequest request = new SearchRequest.Builder()
                .index("ssok-app")
                .query(q -> q.match(m -> m.field("level").query(FieldValue.of("ERROR | WARN"))))
                .source(s -> s
                        .filter(f -> f
                                .includes("@timestamp", "level", "logger", "thread", "message", "app")
                        )
                )
                .build();

        SearchResponse<LogDataDto> response = openSearchClient.search(request, LogDataDto.class);


        return (LogsResponseDto) response.hits().hits().stream()
                .map(hit -> {
                    LogDataDto source = hit.source();
                    LogDto dto = new LogDto();
                    dto.setLogId(hit.id());
                    dto.setApp(source.getApp());
                    dto.setTimestamp(source.getTimestamp());
                    dto.setLevel(source.getLevel());
                    dto.setLogger(source.getLogger());
                    dto.setThread(source.getThread());
                    dto.setMessage(source.getMessage());
                    return dto;
                })
                .collect(Collectors.toList());
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
                .summary(summaryEntity.getSummary())
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
                .summary(summaryDto.getSummary())
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
    public List<LogDto> getLogsByIds(List<String> logIds) throws Exception {
        log.info("로그 ID 목록으로 로그 조회: {}", logIds);

        MgetRequest request = new MgetRequest.Builder()
                .index("ssok-app")
                .ids(logIds)
                .build();

        MgetResponse<LogDataDto> response = openSearchClient.mget(request, LogDataDto.class);

        List<LogDto> result = new ArrayList<>();
        for (var item : response.docs()) {
            if (item.result().found()) {
                var source = item.result().source();
                var dto = new LogDto();
                dto.setLogId(item.result().id());
                dto.setApp(source.getApp());
                dto.setTimestamp(source.getTimestamp());
                dto.setLevel(source.getLevel());
                dto.setLogger(source.getLogger());
                dto.setThread(source.getThread());
                dto.setMessage(source.getMessage());
                result.add(dto);
            }
        }

        return result;
    }

    /**
     * 로그 데이터를 LLM API 요청 형식으로 변환
     */
    @Override
    public List<LogRequestDto> convertToLlmRequestFormat(List<LogDto> logList) {
        return logList.stream()
                .map(logData -> LogRequestDto.builder()
                        .level(logData.getLevel())
                        .logger(logData.getLogger())
                        .thread(logData.getThread())
                        .message(logData.getMessage())
                        .app(logData.getApp())
                        .build())
                .collect(Collectors.toList());
    }

}
