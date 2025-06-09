package kr.ssok.ssom.backend.global.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 애플리케이션 상태 확인을 위한 Health Check Controller
 * 로드 밸런서나 모니터링 시스템에서 사용
 */
@Slf4j
@RestController
public class HealthController {

    /**
     * 루트 경로 요청 처리
     * 잘못된 경로로 인한 NoResourceFoundException 방지
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "message", "SSOM Backend API Server is running",
            "timestamp", LocalDateTime.now(),
            "service", "ssom-backend"
        ));
    }

    /**
     * 헬스 체크 엔드포인트
     * Kubernetes liveness/readiness probe 등에서 사용
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "service", "ssom-backend"
        ));
    }

    /**
     * 간단한 상태 확인 엔드포인트
     * 최소한의 응답으로 빠른 확인
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    /**
     * 애플리케이션 정보 엔드포인트
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "application", "SSOM Backend",
            "version", "1.0-SNAPSHOT",
            "description", "SSOM 알림 백엔드 서비스",
            "timestamp", LocalDateTime.now()
        ));
    }
}
