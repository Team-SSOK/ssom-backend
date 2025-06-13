package kr.ssok.ssom.backend.domain.alert.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 기본 성능 비교 테스트 (Spring Context 없이)
 * 동기 vs 비동기 처리 패턴의 기본적인 성능 차이 측정
 */
public class BasicPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(BasicPerformanceTest.class);

    @Test
    @DisplayName("동기 vs 비동기 처리 패턴 기본 성능 비교")
    void compareBasicProcessingPatterns() {
        log.info("=== 기본 처리 패턴 성능 비교 테스트 시작 ===");
        
        int taskCount = 100; // 처리할 작업 수
        int iterations = 5;  // 테스트 반복 횟수
        
        List<Long> syncTimes = new ArrayList<>();
        List<Long> asyncTimes = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            log.info("{}회차 테스트 진행 중...", i + 1);
            
            // 동기 처리 방식 측정
            long syncTime = measureSynchronousProcessing(taskCount);
            syncTimes.add(syncTime);
            
            // 잠시 대기
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 비동기 처리 방식 측정
            long asyncTime = measureAsynchronousProcessing(taskCount);
            asyncTimes.add(asyncTime);
            
            log.info("{}회차 결과: 동기 {}ms, 비동기 {}ms", i + 1, syncTime, asyncTime);
            
            // 테스트 간 간격
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        printComparisonResults(syncTimes, asyncTimes, taskCount);
    }

    /**
     * 동기 처리 방식 성능 측정
     */
    private long measureSynchronousProcessing(int taskCount) {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < taskCount; i++) {
            // 알림 전송 시뮬레이션 (동기 방식)
            simulateNotificationProcessing();
            
            // 데이터베이스 처리 시뮬레이션
            simulateDatabaseOperation();
        }
        
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    /**
     * 비동기 처리 방식 성능 측정
     */
    private long measureAsynchronousProcessing(int taskCount) {
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < taskCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // 알림 전송 시뮬레이션 (비동기 방식)
                simulateNotificationProcessing();
                
                // 데이터베이스 처리 시뮬레이션
                simulateDatabaseOperation();
            }, executor);
            
            futures.add(future);
        }
        
        // 모든 비동기 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        executor.shutdown();
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    /**
     * 알림 전송 처리 시뮬레이션
     * 실제 FCM/SSE 전송 시간을 모방
     */
    private void simulateNotificationProcessing() {
        try {
            // FCM/SSE 전송 지연 시뮬레이션 (10-50ms)
            Thread.sleep(10 + (int)(Math.random() * 40));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 데이터베이스 처리 시뮬레이션
     * AlertStatus 생성 등의 DB 작업을 모방
     */
    private void simulateDatabaseOperation() {
        try {
            // 데이터베이스 처리 지연 시뮬레이션 (1-5ms)
            Thread.sleep(1 + (int)(Math.random() * 4));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 테스트 결과 비교 출력
     */
    private void printComparisonResults(List<Long> syncTimes, List<Long> asyncTimes, int taskCount) {
        double avgSync = syncTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgAsync = asyncTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        
        long maxSync = syncTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minSync = syncTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxAsync = asyncTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minAsync = asyncTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        
        double syncThroughput = taskCount / (avgSync / 1000.0);
        double asyncThroughput = taskCount / (avgAsync / 1000.0);
        
        log.info("\n" + "=".repeat(70));
        log.info("📊 기본 처리 패턴 성능 비교 결과 ({}개 작업)", taskCount);
        log.info("=".repeat(70));
        
        log.info("동기 처리 방식:");
        log.info("  평균: {}ms | 최대: {}ms | 최소: {}ms", 
                String.format("%.1f", avgSync), maxSync, minSync);
        log.info("  처리량: {} 작업/초", String.format("%.1f", syncThroughput));
        
        log.info("비동기 처리 방식:");
        log.info("  평균: {}ms | 최대: {}ms | 최소: {}ms", 
                String.format("%.1f", avgAsync), maxAsync, minAsync);
        log.info("  처리량: {} 작업/초", String.format("%.1f", asyncThroughput));
        
        if (avgSync > 0 && avgAsync > 0) {
            double improvement = ((avgSync - avgAsync) / avgSync) * 100;
            double throughputImprovement = ((asyncThroughput - syncThroughput) / syncThroughput) * 100;
            
            log.info("\n성능 개선 효과:");
            log.info("  응답시간 개선: {}%", String.format("%.1f", improvement));
            log.info("  처리량 개선: {}%", String.format("%.1f", throughputImprovement));
            
            if (avgAsync < avgSync) {
                log.info("  🚀 비동기 방식이 {}ms 더 빠릅니다!", 
                        String.format("%.1f", avgSync - avgAsync));
            } else {
                log.info("  ⚠️ 동기 방식이 {}ms 더 빠릅니다", 
                        String.format("%.1f", avgAsync - avgSync));
            }
        }
        
        log.info("=".repeat(70));
        
        // 패턴별 특징 설명
        log.info("\n📝 처리 패턴 특징:");
        log.info("동기 방식:");
        log.info("  - 순차적 처리로 예측 가능한 성능");
        log.info("  - 단순한 구조, 디버깅 용이");
        log.info("  - 대량 처리 시 응답시간 증가");
        
        log.info("비동기 방식:");
        log.info("  - 병렬 처리로 빠른 응답시간");
        log.info("  - 시스템 자원 효율적 활용");
        log.info("  - 복잡한 구조, 에러 처리 주의 필요");
    }

    @Test
    @DisplayName("스레드 풀 크기별 성능 비교")
    void compareThreadPoolSizes() {
        log.info("\n=== 스레드 풀 크기별 성능 비교 테스트 ===");
        
        int taskCount = 50;
        int[] threadPoolSizes = {1, 5, 10, 20};
        
        for (int poolSize : threadPoolSizes) {
            long processingTime = measureAsyncProcessingWithPoolSize(taskCount, poolSize);
            double throughput = taskCount / (processingTime / 1000.0);
            
            log.info("스레드 풀 크기 {}: {}ms, {} 작업/초", 
                    poolSize, processingTime, String.format("%.1f", throughput));
        }
    }

    private long measureAsyncProcessingWithPoolSize(int taskCount, int poolSize) {
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < taskCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                simulateNotificationProcessing();
                simulateDatabaseOperation();
            }, executor);
            
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        
        return System.currentTimeMillis() - startTime;
    }
}
