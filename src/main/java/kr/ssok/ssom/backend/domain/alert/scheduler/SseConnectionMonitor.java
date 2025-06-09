package kr.ssok.ssom.backend.domain.alert.scheduler;

import kr.ssok.ssom.backend.domain.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SSE 연결 상태를 모니터링하고 정리하는 스케줄러
 * 비활성 연결을 주기적으로 정리하여 메모리 누수 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseConnectionMonitor {

    private final AlertService alertService;

    /**
     * 5분마다 비활성 SSE 연결 정리
     * 연결이 끊어진 emitter들을 제거하여 메모리 효율성 향상
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    public void cleanupInactiveConnections() {
        try {
            int beforeCount = alertService.getActiveEmitterCount();
            
            alertService.cleanupDisconnectedEmitters();
            
            int afterCount = alertService.getActiveEmitterCount();
            int removedCount = beforeCount - afterCount;
            
            if (removedCount > 0) {
                log.info("[SSE 연결 정리] 비활성 연결 {}개 제거됨. 활성 연결: {}개 -> {}개", 
                        removedCount, beforeCount, afterCount);
            } else {
                log.debug("[SSE 연결 정리] 활성 연결: {}개", afterCount);
            }
            
        } catch (Exception e) {
            log.error("[SSE 연결 정리] 정리 작업 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 1시간마다 SSE 연결 상태 통계 로깅
     * 시스템 상태 모니터링용
     */
    @Scheduled(fixedRate = 3600000) // 1시간 = 3,600,000ms
    public void logConnectionStatistics() {
        try {
            int activeConnections = alertService.getActiveEmitterCount();
            
            log.info("[SSE 연결 통계] 현재 활성 SSE 연결 수: {}개", activeConnections);
            
            // 메모리 사용량 정보 추가
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            log.info("[시스템 리소스] 사용 메모리: {}MB / {}MB ({}%)", 
                    usedMemory / 1024 / 1024, 
                    totalMemory / 1024 / 1024,
                    (usedMemory * 100) / totalMemory);
                    
        } catch (Exception e) {
            log.error("[SSE 연결 통계] 통계 수집 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 애플리케이션 시작 시 초기화 로그
     */
    @Scheduled(initialDelay = 30000, fixedRate = Long.MAX_VALUE) // 30초 후 1회 실행
    public void logInitialization() {
        log.info("[SSE 모니터링] SSE 연결 모니터링 스케줄러가 시작되었습니다.");
        log.info("[SSE 모니터링] 비활성 연결 정리: 5분마다 실행");
        log.info("[SSE 모니터링] 연결 통계 로깅: 1시간마다 실행");
    }
}
