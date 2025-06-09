package kr.ssok.ssom.backend.domain.logging.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.domain.logging.entity.LogSummary;
import kr.ssok.ssom.backend.domain.logging.repository.LogSummaryRepository;
import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.domain.logging.sse.EmitterWithFilter;
import kr.ssok.ssom.backend.global.client.LlmServiceClient;
import kr.ssok.ssom.backend.global.dto.*;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch.core.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingServiceImpl implements LoggingService {

    private final LogSummaryRepository logSummaryRepository;
    private final LlmServiceClient llmServiceClient;
    private final OpenSearchClient openSearchClient;

    private final ObjectMapper objectMapper;

    Map<String, EmitterWithFilter> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 1시간

    /**
     * 서비스 목록 조회
     */
    @Override
    public ServicesResponseDto getServices() {

        try {
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

            return new ServicesResponseDto(result);

        } catch (IOException e) {
            throw new BaseException(BaseResponseStatus.SERVICES_READ_FAILED);
        }

    }

    /**
     * 로그 목록 조회
     */
    @Override
    public LogsResponseDto getLogs(String app, String level) {

        try {
            // 동적 bool 쿼리 빌더
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            if (StringUtils.hasText(app)) {
                boolQuery.must(m -> m.match(match -> match.field("app.keyword").query(FieldValue.of(app))));
            }

            if (StringUtils.hasText(level)) {
                boolQuery.must(m -> m.match(match -> match.field("level").query(FieldValue.of(level))));
            } else {
                // level이 없으면 기본적으로 ERROR + WARN
                boolQuery.must(m -> m.terms(t -> t.field("level.keyword").terms(
                        ts -> ts.value(List.of(FieldValue.of("ERROR"), FieldValue.of("WARN")))
                )));
            }

            // 요청
            SearchRequest request = new SearchRequest.Builder()
                    .index("ssok-app")
                    .query(q -> q.bool(boolQuery.build()))
                    .sort(s -> s
                            .field(f -> f
                                    .field("@timestamp")
                                    .order(SortOrder.Desc)
                            )
                    )
                    .source(s -> s
                            .filter(f -> f
                                    .includes("@timestamp", "level", "logger", "thread", "message", "app")
                            )
                    )
                    .size(10000) // 1000개 제한
                    .build();

            SearchResponse<LogDataDto> response = openSearchClient.search(request, LogDataDto.class);

            List<LogDto> result = response.hits().hits().stream()
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

            // 중복 제거: 연속된 같은 message만 하나만 남기기
            List<LogDto> deduplicated = new ArrayList<>();
            LogDto prev = null;
            for (LogDto current : result) {
                if (prev == null || !current.getMessage().equals(prev.getMessage())) {
                    deduplicated.add(current);
                }
                prev = current;
            }

            return new LogsResponseDto(deduplicated);
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.LOGS_READ_FAILED);
        }

    }

    /**
     * 로그 SSE 구독
     */
    @Override
    public SseEmitter subscribe(String employeeId, String app, String level, HttpServletResponse response){
        log.info("[로그 SSE 구독] 서비스 진입");

        // 1. 기존 emitter 제거
        if (emitters.containsKey(employeeId)) {
            EmitterWithFilter wrapper = emitters.remove(employeeId);
            if (wrapper != null) {
                try {
                    SseEmitter oldEmitter = wrapper.getEmitter();
                    oldEmitter.complete();  // 또는 .completeWithError(new IOException("reconnect"));
                    log.info("[로그 SSE 구독] 기존 emitter 제거 완료");
                } catch (Exception e) {
                    log.warn("[로그 SSE 구독] 기존 emitter 제거 중 예외 발생: {}", e.getMessage());
                }
            }
        }

        // 2. 새로운 emitter 생성
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        EmitterWithFilter filteredEmitter = new EmitterWithFilter(emitter, app, level);
        emitters.put(employeeId, filteredEmitter);

        response.setHeader("X-Accel-Buffering", "no");

        // 3. 연결 종료 혹은 에러 발생 시 제거
        emitter.onCompletion(() -> emitters.remove(employeeId));
        emitter.onTimeout(() -> emitters.remove(employeeId));
        emitter.onError((e) -> emitters.remove(employeeId));

        // 4. 연결 테스트용 초기 이벤트 전송
        try {
            emitter.send(SseEmitter.event().name("LOGGING_INIT").data("connected"));
        } catch (IOException | IllegalStateException e) {
            emitters.remove(employeeId);
            emitter.completeWithError(e);
            throw new BaseException(BaseResponseStatus.SSE_INIT_ERROR);
        }

        log.info("[로그 SSE 구독] SSE 연결 완료: employeeId: {}", employeeId);

        return emitter;
    }

    /**
     * 로그 SSE 전송 (실시간으로 뜨는 로그를 하나씩)
     */
    public void sendLogToUsers(LogDto logDto) {
        log.info("[로그 SSE 전송] 서비스 진입");

        List<String> deadEmitters = new ArrayList<>();

        // 현재 연결된 모든 emitter들에 대해
        for (Map.Entry<String, EmitterWithFilter> entry : emitters.entrySet()) {
            String employeeId = entry.getKey();
            EmitterWithFilter e = entry.getValue();

            // 로그 목록 조회 필터링 조건 체크
            boolean appMatches = e.getAppFilter() == null || e.getAppFilter().equalsIgnoreCase(logDto.getApp());
            boolean levelMatches = e.getLevelFilter() == null || e.getLevelFilter().equalsIgnoreCase(logDto.getLevel());

            // 필터링 조건 모두 만족하는 emitter에게만 전송
            if (appMatches && levelMatches) {
                try {
                    e.getEmitter().send(SseEmitter.event()
                            .name("LOGGING")
                            .data(logDto));
                } catch (IOException ex) {
                    deadEmitters.add(employeeId);
                }
            }
        }

        deadEmitters.forEach(emitters::remove);

    }

    /**
     * 오픈서치에서 보내주는 실시간 로그
     */
    @Override
    public void createOpensearchAlert(String requestStr) {
        log.info("[오픈서치 실시간 로그] 서비스 진입 : requestStr = {}", requestStr);

        try {
            if (requestStr == null || requestStr.isEmpty()) {
                log.warn("[오픈서치 실시간 로그] 전달받은 원본 데이터가 비어있습니다.");
                return;
            }

            List<LogDto> loggingList = parseRawStringToDtoList(requestStr);

            if (loggingList == null || loggingList.isEmpty()) {
                log.warn("[오픈서치 실시간 로그] Json 파싱 결과 실시간 로그 리스트가 비어있습니다.");
                return;
            }

            for (LogDto loggingRequest : loggingList) {
                try {
                    sendLogToUsers(loggingRequest);
                } catch (BaseException be) {
                    log.error("[오픈서치 실시간 로그] 개별 실시간 로그 처리 실패 : loggingRequest = {}, error = {}", loggingRequest, be.getMessage());
                } catch (Exception e) {
                    log.error("[오픈서치 실시간 로그] 실시간 로그 처리 중 예외 발생 : loggingRequest = {}, error = {}", loggingRequest, e.getMessage(), e);
                }
            }

            log.info("[오픈서치 실시간 로그] 전체 {}건 서비스 처리 완료", loggingList.size());

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[오픈서치 실시간 로그] 전체 처리 중 예외 발생 - error = {}", e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 오픈서치에서 보내주는 데이터 파싱
     */
    private List<LogDto> parseRawStringToDtoList(String raw) {
        log.info("[JSON Parsing] 진행 중 ...");

        try {
            if (raw == null || raw.isBlank()) {
                log.warn("[JSON Parsing] 전달받은 원본 문자열이 비어있습니다.");
                throw new BaseException(BaseResponseStatus.PARSING_ERROR);
            }

            // 공백 및 개행 제거
            String fixed = raw.trim();
            fixed = fixed.replaceAll(",\\s*]", "]");

            if (!fixed.trim().startsWith("[")) {
                log.warn("[JSON Parsing] 전달받은 원본 문자열의 형식이 상이합니다.");
                throw new BaseException(BaseResponseStatus.PARSING_ERROR);
            }

            return objectMapper.readValue(fixed, new TypeReference<List<LogDto>>() {});

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[JSON Parsing] JSON 파싱 중 예외 발생 : input = {}", raw, e);
            throw new BaseException(BaseResponseStatus.PARSING_ERROR);
        }
    }

    /**
     * 로그 상세 조회 - 이전에 생성한 LLM 요약 반환
     */
    @Override
    public LogSummaryMessageDto getLogAnalysisInfo(String logId) {

        // 로그 아이디로 OpenSearch에 로그 조회
        LogDto logDto = getLogById(logId);

        // 로그 메시지로 DB 조회
        String logMessage = logDto.getMessage();
        Optional<LogSummary> summaryOpt = logSummaryRepository.findByLogMessage(logMessage);

        // 없을 시 아무것도 반환하지 않음
        if (summaryOpt.isEmpty()) {
            throw new BaseException(BaseResponseStatus.LOG_SUMMARY_NOT_FOUND);
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
                .solutionDetail(summaryEntity.getSolutionDetail())
                .build();

        return summaryDto;
    }

    /**
     * 로그 상세 조회 - 새롭게 생성한 LLM 요약 반환
     */
    @Override
    public LogSummaryMessageDto analyzeLog(LogDto request) {

        // LLM 쪽으로 api 요청
        LogRequestDto requestDto = LogRequestDto.builder()
                .level(request.getLevel())
                .logger(request.getLogger())
                .thread(request.getThread())
                .message(request.getMessage())
                .app(request.getApp())
                .build();
        LlmApiRequestDto llmRequestDto = LlmApiRequestDto.builder()
                .log(List.of(requestDto))
                .build();

        LlmApiResponseDto<LogSummaryResponseDto> llmResponseDto;
        try {
            llmResponseDto = llmServiceClient.summarizeLog(llmRequestDto);
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.LLM_SUMMARY_FAILED);
        }

        // 응답에서 요약 메시지 추출
        LogSummaryMessageDto summaryDto = llmResponseDto.getResult().get(0).getMessage();

        // DB에 저장
        try {
            LogSummary summaryEntity = LogSummary.builder()
                    .logId(request.getLogId())
                    .logMessage(request.getMessage())
                    .summary(summaryDto.getSummary())
                    .fileLocation(summaryDto.getLocation().getFile())
                    .functionLocation(summaryDto.getLocation().getFunction())
                    .solution(summaryDto.getSolution())
                    .solutionDetail(summaryDto.getSolutionDetail())
                    .build();
            logSummaryRepository.save(summaryEntity);
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.LLM_SUMMARY_SAVE_FAILED);
        }

        // 분석 내용 반환
        return summaryDto;
    }

    /**
     * 로그 ID로 로그 데이터 조회
     */
    @Override
    public LogDto getLogById(String logId) {
        log.info("로그 ID로 단일 로그 조회: {}", logId);

        try {
            GetRequest request = new GetRequest.Builder()
                    .index("ssok-app")
                    .id(logId)
                    .build();

            GetResponse<LogDataDto> response = openSearchClient.get(request, LogDataDto.class);

            if (!response.found()) {
                throw new BaseException(BaseResponseStatus.LOG_NOT_FOUND);
            }

            LogDataDto source = response.source();
            return LogDto.builder()
                    .logId(response.id())
                    .app(source.getApp())
                    .timestamp(source.getTimestamp())
                    .level(source.getLevel())
                    .logger(source.getLogger())
                    .thread(source.getThread())
                    .message(source.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("로그 조회 중 예외 발생: {}", e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.LOG_NOT_FOUND);
        }
    }

    /**
     * 로그 ID 목록으로 로그 데이터 조회 (Issue 생성용)
     */
    @Override
    public List<LogDto> getLogsByIds(List<String> logIds) {
        log.info("로그 ID 목록으로 로그 조회: {}", logIds);

        try {
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
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.LOG_NOT_FOUND);
        }

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
