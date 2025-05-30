package kr.ssok.ssom.backend.domain.logging.controller;

import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.global.dto.LogRequestDto;
import kr.ssok.ssom.backend.global.dto.LogSummaryMessageDto;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<BaseResponse<LogsResponseDto>> getLogs() {

        log.info("로그 목록 조회 요청");
        LogsResponseDto response = loggingService.getLogs();

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));

    }

    // 로그 상세 조회 - 이전에 생성한 LLM 요약 반환
    @GetMapping("/{logId}")
    public ResponseEntity<BaseResponse<LogSummaryMessageDto>> getLogInfo(@PathVariable String logId) {

        log.info("로그 상세 조회 요청 - 이전에 생성한 LLM 요약 반환(logId: {})", logId);
        LogSummaryMessageDto response = loggingService.getLogInfo(logId);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));

    }

    // 로그 상세 조회 - 새롭게 생성한 로그 LLM 요약 반환
    @PostMapping("/{logId}")
    public ResponseEntity<BaseResponse<LogSummaryMessageDto>> summarizeLog(@PathVariable String logId, @RequestBody LogDto request) {

        log.info("LLM 요약 요청(logId: {})", logId);
        LogSummaryMessageDto response = loggingService.summarizeLog(request);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));

    }
}
