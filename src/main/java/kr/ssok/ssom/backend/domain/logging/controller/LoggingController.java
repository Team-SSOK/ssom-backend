package kr.ssok.ssom.backend.domain.logging.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import kr.ssok.ssom.backend.global.dto.LogSummaryMessageDto;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/logging")
@RequiredArgsConstructor
public class LoggingController {

    private final LoggingService loggingService;

    // 서비스 목록 조회
    @GetMapping("/services")
    public ResponseEntity<BaseResponse<ServicesResponseDto>> getServices() {

        log.info("서비스 목록 조회 요청");
        ServicesResponseDto response = loggingService.getServices();

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));

    }

    // 로그 목록 조회
    @GetMapping
    public ResponseEntity<BaseResponse<LogsResponseDto>> getLogs(@RequestParam(required = false) String app,
                                                                 @RequestParam(required = false) String level) throws Exception {

        log.info("로그 목록 조회 요청: app={}, level={}", app, level);
        LogsResponseDto response = loggingService.getLogs(app, level);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));

    }

    // 로그 목록 조회 (무한 스크롤 방식)
    @GetMapping("/infinitescroll")
    public ResponseEntity<BaseResponse<LogsScrollResponseDto>> getLogsInfiniteScroll(
            @RequestParam(required = false) String app,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String searchAfterTimestamp,
            @RequestParam(required = false) String searchAfterId) throws Exception {

        log.info("로그 목록 조회 요청: app={}, level={}, searchAfterTimestamp={}, searchAfterId={}",
                app, level, searchAfterTimestamp, searchAfterId);

        LogsScrollResponseDto response = loggingService.getLogsInfiniteScroll(app, level, searchAfterTimestamp, searchAfterId);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));
    }

    // 오픈서치에서 보내주는 실시간 로그
    @PostMapping("/opensearch")
    public ResponseEntity<BaseResponse<Void>> sendOpensearchLogging(@RequestBody String requestStr) {
        log.info("[오픈서치 대시보드 알림] 컨트롤러 진입");

        loggingService.createOpensearchAlert(requestStr);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new BaseResponse<>(BaseResponseStatus.SUCCESS));
    }

    // 로그 SSE 구독
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
                                @RequestParam(value = "app", required = false) String appFilter,
                                @RequestParam(value = "level", required = false) String levelFilter,
                                HttpServletResponse response) {
        
        // 인증되지 않은 사용자 처리
        if (userPrincipal == null) {
            log.error("SSE 구독 실패 - 인증되지 않은 사용자");
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED);
        }
        
        return loggingService.subscribe(userPrincipal.getEmployeeId(), appFilter, levelFilter, response);
    }

    // 로그 상세 조회 - 이전에 생성한 LLM 요약 반환
    @GetMapping("/analysis/{logId}")
    public ResponseEntity<BaseResponse<LogSummaryMessageDto>> getLogAnalysisInfo(@PathVariable String logId) {

        log.info("로그 상세 조회 요청 - 이전에 생성한 LLM 요약 반환(logId: {})", logId);
        LogSummaryMessageDto response = loggingService.getLogAnalysisInfo(logId);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));
    }

    // 로그 상세 조회 - 새롭게 생성한 로그 LLM 요약 반환
    @PostMapping("/analysis")
    public ResponseEntity<BaseResponse<LogSummaryMessageDto>> summarizeLog(@RequestBody LogDto request) {

        log.info("LLM 요약 요청(logId: {})", request.getLogId());
        LogSummaryMessageDto response = loggingService.analyzeLog(request);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));

    }

    // 로그 상세 조회
    @GetMapping("/{logId}")
    public ResponseEntity<BaseResponse<LogDto>> getLogInfo(@PathVariable String logId) {

        log.info("로그 상세 조회 요청 (logId: {})", logId);
        LogDto response = loggingService.getLogById(logId);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));
    }

}
