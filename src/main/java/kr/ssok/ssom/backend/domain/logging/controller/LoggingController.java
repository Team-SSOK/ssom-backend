package kr.ssok.ssom.backend.domain.logging.controller;

import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/logging")
@RequiredArgsConstructor
public class LoggingController {

    private final LoggingService loggingService;

    // 서비스 목록 조회
    @GetMapping("/services")
    public ResponseEntity<BaseResponse<>> getServices() {

    }

    // 로그 목록 조회
    @GetMapping
    public ResponseEntity<BaseResponse<>> getLogs() {

    }

    // 로그 상세 조회 - 로그 LLM 분석
    @PostMapping("/{logId}")
    public ResponseEntity<BaseResponse<>> analyzeLog() {

    }
}
