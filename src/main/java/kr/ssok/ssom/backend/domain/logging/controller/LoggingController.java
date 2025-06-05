package kr.ssok.ssom.backend.domain.logging.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.alert.service.AlertService;
import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import kr.ssok.ssom.backend.global.dto.LogRequestDto;
import kr.ssok.ssom.backend.global.dto.LogSummaryMessageDto;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/logging")
@RequiredArgsConstructor
public class LoggingController {

    private final LoggingService loggingService;
    private final AlertService alertService;

    // 서비스 목록 조회
    @GetMapping("/services")
    public ResponseEntity<BaseResponse<ServicesResponseDto>> getServices() throws IOException {

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

    // 로그 SSE 구독
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
                                @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId,
                                HttpServletResponse response) {
        return loggingService.subscribe(userPrincipal.getEmployeeId(), lastEventId, response);
    }

    // 로그 상세 조회 - 이전에 생성한 LLM 요약 반환
    @GetMapping("/{logId}")
    public ResponseEntity<BaseResponse<LogSummaryMessageDto>> getLogInfo(@PathVariable String logId) {

        log.info("로그 상세 조회 요청 - 이전에 생성한 LLM 요약 반환(logId: {})", logId);
        LogSummaryMessageDto response = loggingService.getLogInfo(logId);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));
    }

    // 로그 상세 조회 - 새롭게 생성한 로그 LLM 요약 반환
    @PostMapping
    public ResponseEntity<BaseResponse<LogSummaryMessageDto>> summarizeLog(@RequestBody LogDto request) {

        log.info("LLM 요약 요청(logId: {})", request.getLogId());
        LogSummaryMessageDto response = loggingService.summarizeLog(request);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));

    }
}
