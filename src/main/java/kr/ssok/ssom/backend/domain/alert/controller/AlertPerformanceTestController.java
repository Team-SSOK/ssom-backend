package kr.ssok.ssom.backend.domain.alert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ssok.ssom.backend.domain.alert.dto.*;
import kr.ssok.ssom.backend.domain.alert.service.AlertPerformanceTestService;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Alert 성능 테스트용 컨트롤러
 * JMeter를 사용한 동기/비동기 처리 성능 비교용 API 제공
 */
@Tag(name = "Alert Performance Test API", description = "알림 성능 테스트를 위한 API (동기/비동기 비교)")
@RestController
@RequestMapping("/api/alert/performance-test")
@RequiredArgsConstructor
@Slf4j
public class AlertPerformanceTestController {

    private final AlertPerformanceTestService alertPerformanceTestService;

    // ======================================
    // 동기 처리 테스트 API들
    // ======================================

    @Operation(summary = "동기 처리 - 그라파나 알림 테스트", 
               description = "기존 동기 방식으로 그라파나 알림을 처리합니다. (성능 테스트용)")
    @PostMapping("/sync/grafana")
    public ResponseEntity<BaseResponse<Map<String, Object>>> testSyncGrafanaAlert(
            @RequestBody AlertGrafanaRequestDto requestDto,
            @Parameter(description = "알림 대상 사용자 수 (기본값: 모든 사용자)") 
            @RequestParam(defaultValue = "0") int targetUserCount) {
        
        long startTime = System.currentTimeMillis();
        log.info("[성능 테스트 - 동기] 그라파나 알림 처리 시작 - targetUserCount: {}", targetUserCount);

        try {
            // 동기 처리 실행
            Map<String, Object> result = alertPerformanceTestService.processSyncGrafanaAlert(requestDto, targetUserCount);
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            result.put("processingTime", processingTime);
            result.put("processingType", "SYNC");
            result.put("timestamp", LocalDateTime.now());
            
            log.info("[성능 테스트 - 동기] 그라파나 알림 처리 완료 - 소요시간: {}ms", processingTime);
            
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, result));
            
        } catch (Exception e) {
            log.error("[성능 테스트 - 동기] 그라파나 알림 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Operation(summary = "동기 처리 - 오픈서치 알림 테스트", 
               description = "기존 동기 방식으로 오픈서치 알림을 처리합니다. (성능 테스트용)")
    @PostMapping("/sync/opensearch")
    public ResponseEntity<BaseResponse<Map<String, Object>>> testSyncOpensearchAlert(
            @RequestBody String requestStr,
            @Parameter(description = "알림 대상 사용자 수 (기본값: 모든 사용자)") 
            @RequestParam(defaultValue = "0") int targetUserCount) {
        
        long startTime = System.currentTimeMillis();
        log.info("[성능 테스트 - 동기] 오픈서치 알림 처리 시작 - targetUserCount: {}", targetUserCount);

        try {
            // 동기 처리 실행
            Map<String, Object> result = alertPerformanceTestService.processSyncOpensearchAlert(requestStr, targetUserCount);
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            result.put("processingTime", processingTime);
            result.put("processingType", "SYNC");
            result.put("timestamp", LocalDateTime.now());
            
            log.info("[성능 테스트 - 동기] 오픈서치 알림 처리 완료 - 소요시간: {}ms", processingTime);
            
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, result));
            
        } catch (Exception e) {
            log.error("[성능 테스트 - 동기] 오픈서치 알림 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Operation(summary = "동기 처리 - DevOps 알림 테스트", 
               description = "기존 동기 방식으로 DevOps 알림을 처리합니다. (성능 테스트용)")
    @PostMapping("/sync/devops")
    public ResponseEntity<BaseResponse<Map<String, Object>>> testSyncDevopsAlert(
            @RequestBody AlertDevopsRequestDto requestDto,
            @Parameter(description = "알림 대상 사용자 수 (기본값: 모든 사용자)") 
            @RequestParam(defaultValue = "0") int targetUserCount) {
        
        long startTime = System.currentTimeMillis();
        log.info("[성능 테스트 - 동기] DevOps 알림 처리 시작 - targetUserCount: {}", targetUserCount);

        try {
            // 동기 처리 실행
            Map<String, Object> result = alertPerformanceTestService.processSyncDevopsAlert(requestDto, targetUserCount);
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            result.put("processingTime", processingTime);
            result.put("processingType", "SYNC");
            result.put("timestamp", LocalDateTime.now());
            
            log.info("[성능 테스트 - 동기] DevOps 알림 처리 완료 - 소요시간: {}ms", processingTime);
            
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, result));
            
        } catch (Exception e) {
            log.error("[성능 테스트 - 동기] DevOps 알림 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ======================================
    // 비동기 처리 테스트 API들
    // ======================================

    @Operation(summary = "비동기 처리 - 그라파나 알림 테스트", 
               description = "Kafka 기반 비동기 방식으로 그라파나 알림을 처리합니다. (성능 테스트용)")
    @PostMapping("/async/grafana")
    public ResponseEntity<BaseResponse<Map<String, Object>>> testAsyncGrafanaAlert(
            @RequestBody AlertGrafanaRequestDto requestDto,
            @Parameter(description = "알림 대상 사용자 수 (기본값: 모든 사용자)") 
            @RequestParam(defaultValue = "0") int targetUserCount) {
        
        long startTime = System.currentTimeMillis();
        log.info("[성능 테스트 - 비동기] 그라파나 알림 처리 시작 - targetUserCount: {}", targetUserCount);

        try {
            // 비동기 처리 실행 (즉시 반환)
            Map<String, Object> result = alertPerformanceTestService.processAsyncGrafanaAlert(requestDto, targetUserCount);
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            result.put("processingTime", processingTime);
            result.put("processingType", "ASYNC");
            result.put("timestamp", LocalDateTime.now());
            
            log.info("[성능 테스트 - 비동기] 그라파나 알림 처리 완료 - 소요시간: {}ms", processingTime);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED) // 202 Accepted
                    .body(new BaseResponse<>(BaseResponseStatus.SUCCESS, result));
            
        } catch (Exception e) {
            log.error("[성능 테스트 - 비동기] 그라파나 알림 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Operation(summary = "비동기 처리 - 오픈서치 알림 테스트", 
               description = "Kafka 기반 비동기 방식으로 오픈서치 알림을 처리합니다. (성능 테스트용)")
    @PostMapping("/async/opensearch")
    public ResponseEntity<BaseResponse<Map<String, Object>>> testAsyncOpensearchAlert(
            @RequestBody String requestStr,
            @Parameter(description = "알림 대상 사용자 수 (기본값: 모든 사용자)") 
            @RequestParam(defaultValue = "0") int targetUserCount) {
        
        long startTime = System.currentTimeMillis();
        log.info("[성능 테스트 - 비동기] 오픈서치 알림 처리 시작 - targetUserCount: {}", targetUserCount);

        try {
            // 비동기 처리 실행 (즉시 반환)
            Map<String, Object> result = alertPerformanceTestService.processAsyncOpensearchAlert(requestStr, targetUserCount);
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            result.put("processingTime", processingTime);
            result.put("processingType", "ASYNC");
            result.put("timestamp", LocalDateTime.now());
            
            log.info("[성능 테스트 - 비동기] 오픈서치 알림 처리 완료 - 소요시간: {}ms", processingTime);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED) // 202 Accepted
                    .body(new BaseResponse<>(BaseResponseStatus.SUCCESS, result));
            
        } catch (Exception e) {
            log.error("[성능 테스트 - 비동기] 오픈서치 알림 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Operation(summary = "비동기 처리 - DevOps 알림 테스트", 
               description = "Kafka 기반 비동기 방식으로 DevOps 알림을 처리합니다. (성능 테스트용)")
    @PostMapping("/async/devops")
    public ResponseEntity<BaseResponse<Map<String, Object>>> testAsyncDevopsAlert(
            @RequestBody AlertDevopsRequestDto requestDto,
            @Parameter(description = "알림 대상 사용자 수 (기본값: 모든 사용자)") 
            @RequestParam(defaultValue = "0") int targetUserCount) {
        
        long startTime = System.currentTimeMillis();
        log.info("[성능 테스트 - 비동기] DevOps 알림 처리 시작 - targetUserCount: {}", targetUserCount);

        try {
            // 비동기 처리 실행 (즉시 반환)
            Map<String, Object> result = alertPerformanceTestService.processAsyncDevopsAlert(requestDto, targetUserCount);
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            result.put("processingTime", processingTime);
            result.put("processingType", "ASYNC");
            result.put("timestamp", LocalDateTime.now());
            
            log.info("[성능 테스트 - 비동기] DevOps 알림 처리 완료 - 소요시간: {}ms", processingTime);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED) // 202 Accepted
                    .body(new BaseResponse<>(BaseResponseStatus.SUCCESS, result));
            
        } catch (Exception e) {
            log.error("[성능 테스트 - 비동기] DevOps 알림 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ======================================
    // 테스트 데이터 생성 및 모니터링 API들
    // ======================================

    @Operation(summary = "테스트 데이터 생성", 
               description = "성능 테스트를 위한 샘플 알림 데이터를 생성합니다.")
    @PostMapping("/generate-test-data")
    public ResponseEntity<BaseResponse<List<Object>>> generateTestData(
            @Parameter(description = "생성할 알림 종류") 
            @RequestParam String alertType,
            @Parameter(description = "생성할 알림 개수") 
            @RequestParam(defaultValue = "1") int count) {
        
        log.info("[테스트 데이터 생성] alertType: {}, count: {}", alertType, count);

        try {
            List<Object> testData = alertPerformanceTestService.generateTestData(alertType, count);
            
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, testData));
            
        } catch (Exception e) {
            log.error("[테스트 데이터 생성] 실패 - alertType: {}, count: {}", alertType, count, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Operation(summary = "시스템 상태 조회", 
               description = "현재 시스템의 상태를 조회합니다. (SSE 연결 수, Kafka 큐 상태 등)")
    @GetMapping("/system-status")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getSystemStatus() {
        
        log.info("[시스템 상태 조회] 요청");

        try {
            Map<String, Object> systemStatus = alertPerformanceTestService.getSystemStatus();
            
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, systemStatus));
            
        } catch (Exception e) {
            log.error("[시스템 상태 조회] 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Operation(summary = "성능 메트릭 조회", 
               description = "최근 처리된 알림들의 성능 메트릭을 조회합니다.")
    @GetMapping("/metrics")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getPerformanceMetrics(
            @Parameter(description = "조회할 시간 범위 (분 단위)") 
            @RequestParam(defaultValue = "10") int durationMinutes) {
        
        log.info("[성능 메트릭 조회] durationMinutes: {}", durationMinutes);

        try {
            Map<String, Object> metrics = alertPerformanceTestService.getPerformanceMetrics(durationMinutes);
            
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, metrics));
            
        } catch (Exception e) {
            log.error("[성능 메트릭 조회] 실패 - durationMinutes: {}", durationMinutes, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Operation(summary = "테스트 환경 초기화", 
               description = "성능 테스트를 위해 시스템을 초기화합니다.")
    @PostMapping("/reset")
    public ResponseEntity<BaseResponse<Map<String, Object>>> resetTestEnvironment() {
        
        log.info("[테스트 환경 초기화] 요청");

        try {
            Map<String, Object> result = alertPerformanceTestService.resetTestEnvironment();
            
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, result));
            
        } catch (Exception e) {
            log.error("[테스트 환경 초기화] 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }
}
