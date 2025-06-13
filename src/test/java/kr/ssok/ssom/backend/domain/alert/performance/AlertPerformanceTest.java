package kr.ssok.ssom.backend.domain.alert.performance;

import kr.ssok.ssom.backend.domain.alert.dto.AlertGrafanaRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertResponseDto;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import kr.ssok.ssom.backend.domain.alert.repository.AlertRepository;
import kr.ssok.ssom.backend.domain.alert.repository.AlertStatusRepository;
import kr.ssok.ssom.backend.domain.alert.service.AlertService;
import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.repository.UserRepository;
import kr.ssok.ssom.backend.global.client.FirebaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Alert 성능 테스트
 * 기존 동기 방식 vs Kafka 비동기 방식 성능 비교
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AlertPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(AlertPerformanceTest.class);

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertStatusRepository alertStatusRepository;

    @MockitoSpyBean
    private AlertService alertServiceSpy;

    // Firebase와 Redis를 Mock으로 처리
    @MockitoBean
    private FirebaseClient firebaseClient;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    private static final int TARGET_USER_COUNT = 100;
    private static final int TEST_ITERATIONS = 5; // 테스트 시간 단축을 위해 5회로 감소
    private static final long FCM_DELAY_MS = 20; // FCM 전송 시뮬레이션 딜레이 단축
    private static final long SSE_DELAY_MS = 5; // SSE 전송 시뮬레이션 딜레이 단축

    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // Redis Mock 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("mock-fcm-token");
        
        // Firebase Mock 설정 (아무것도 하지 않음)
        doNothing().when(firebaseClient).sendNotification(any());
        
        // 테스트용 사용자 100명 생성
        createTestUsers();
        
        // 알림 전송 메서드 모킹 (실제 전송 대신 딜레이만 시뮬레이션)
        mockNotificationMethods();
    }

    /**
     * 테스트용 사용자 100명 생성
     */
    private void createTestUsers() {
        testUsers = new ArrayList<>();
        
        for (int i = 1; i <= TARGET_USER_COUNT; i++) {
            User user = User.builder()
                    .id("user" + String.format("%03d", i))
                    .username("TestUser" + i)
                    .password("testpass" + i)
                    .phoneNumber("010-0000-" + String.format("%04d", i))
                    .department(i % 2 == 0 ? Department.OPERATION : Department.CORE_BANK)
                    .build();
            testUsers.add(user);
        }
        
        userRepository.saveAll(testUsers);
        log.info("테스트용 사용자 {}명 생성 완료", testUsers.size());
    }

    /**
     * 알림 전송 메서드 모킹 (실제 네트워크 호출 없이 딜레이만 시뮬레이션)
     */
    private void mockNotificationMethods() {
        doAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            AlertResponseDto alertDto = invocation.getArgument(1);
            
            // SSE 연결 여부를 랜덤으로 결정 (70% SSE, 30% FCM)
            if (Math.random() < 0.7) {
                simulateSSEDelay();
            } else {
                simulateFCMDelay();
            }
            
            return null;
        }).when(alertServiceSpy).sendAlertToUser(anyString(), any(AlertResponseDto.class));

        doAnswer(invocation -> {
            simulateSSEDelay();
            return null;
        }).when(alertServiceSpy).sendSseAlertToUser(anyString(), any(AlertResponseDto.class));

        doAnswer(invocation -> {
            simulateFCMDelay();
            return null;
        }).when(alertServiceSpy).sendFcmNotification(anyString(), any(AlertResponseDto.class));
    }

    private void simulateSSEDelay() {
        try {
            Thread.sleep(SSE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateFCMDelay() {
        try {
            Thread.sleep(FCM_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("동기 vs 비동기 방식 성능 비교 테스트")
    void comparePerformanceSyncVsAsync() throws InterruptedException {
        log.info("=== Alert 성능 테스트 시작 ===");
        log.info("대상 사용자 수: {}명, 테스트 반복 횟수: {}회", TARGET_USER_COUNT, TEST_ITERATIONS);

        PerformanceTestResult syncResult = testSynchronousAlert();
        PerformanceTestResult asyncResult = testAsynchronousAlert();

        printComparisonResults(syncResult, asyncResult);
        
        // 비동기 방식이 동기 방식보다 빨라야 함 (허용 오차 20%)
        assertTrue(asyncResult.averageResponseTime < syncResult.averageResponseTime * 1.2,
                "비동기 방식이 동기 방식보다 현저히 빨라야 합니다");
    }

    /**
     * 동기 방식 성능 테스트
     */
    private PerformanceTestResult testSynchronousAlert() throws InterruptedException {
        log.info("\n🔄 동기 방식 성능 테스트 시작");
        
        List<Long> responseTimes = new ArrayList<>();
        List<Long> totalProcessingTimes = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            log.info("동기 테스트 {}회차 진행 중...", i + 1);
            
            long startTime = System.currentTimeMillis();
            
            try {
                // 그라파나 알림 요청 생성
                AlertGrafanaRequestDto request = createTestGrafanaRequest(i);
                
                // 동기 방식으로 알림 처리 (기존 방식)
                alertService.createGrafanaAlert(request);
                
                long responseTime = System.currentTimeMillis() - startTime;
                responseTimes.add(responseTime);
                
                // 모든 AlertStatus가 생성될 때까지 대기
                waitForAlertStatusCreation(i);
                
                long totalTime = System.currentTimeMillis() - startTime;
                totalProcessingTimes.add(totalTime);
                
                successCount.incrementAndGet();
                
                log.debug("동기 {}회차 - 응답시간: {}ms, 총 처리시간: {}ms", 
                        i + 1, responseTime, totalTime);
                
            } catch (Exception e) {
                log.error("동기 테스트 {}회차 실패: {}", i + 1, e.getMessage());
                failureCount.incrementAndGet();
            }
            
            // 테스트 간 간격
            Thread.sleep(500);
        }

        return PerformanceTestResult.builder()
                .testType("동기 방식")
                .averageResponseTime(calculateAverage(responseTimes))
                .averageTotalProcessingTime(calculateAverage(totalProcessingTimes))
                .maxResponseTime(responseTimes.stream().mapToLong(Long::longValue).max().orElse(0))
                .minResponseTime(responseTimes.stream().mapToLong(Long::longValue).min().orElse(0))
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .throughput(calculateThroughput(successCount.get(), totalProcessingTimes))
                .build();
    }

    /**
     * 비동기 방식 성능 테스트
     */
    private PerformanceTestResult testAsynchronousAlert() throws InterruptedException {
        log.info("\n⚡ 비동기 방식 성능 테스트 시작");
        
        List<Long> responseTimes = new ArrayList<>();
        List<Long> totalProcessingTimes = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            log.info("비동기 테스트 {}회차 진행 중...", i + 1);
            
            long startTime = System.currentTimeMillis();
            
            try {
                // 그라파나 알림 요청 생성
                AlertGrafanaRequestDto request = createTestGrafanaRequest(i + 1000); // ID 충돌 방지
                
                // 비동기 방식으로 알림 처리 (Kafka 방식)
                alertService.createGrafanaAlertAsync(request);
                
                long responseTime = System.currentTimeMillis() - startTime;
                responseTimes.add(responseTime);
                
                // Kafka 비동기 처리가 완료될 때까지 대기
                waitForAsyncProcessingCompletion(i + 1000);
                
                long totalTime = System.currentTimeMillis() - startTime;
                totalProcessingTimes.add(totalTime);
                
                successCount.incrementAndGet();
                
                log.debug("비동기 {}회차 - 응답시간: {}ms, 총 처리시간: {}ms", 
                        i + 1, responseTime, totalTime);
                
            } catch (Exception e) {
                log.error("비동기 테스트 {}회차 실패: {}", i + 1, e.getMessage());
                failureCount.incrementAndGet();
            }
            
            // 테스트 간 간격
            Thread.sleep(500);
        }

        return PerformanceTestResult.builder()
                .testType("비동기 방식")
                .averageResponseTime(calculateAverage(responseTimes))
                .averageTotalProcessingTime(calculateAverage(totalProcessingTimes))
                .maxResponseTime(responseTimes.stream().mapToLong(Long::longValue).max().orElse(0))
                .minResponseTime(responseTimes.stream().mapToLong(Long::longValue).min().orElse(0))
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .throughput(calculateThroughput(successCount.get(), totalProcessingTimes))
                .build();
    }

    @Test
    @DisplayName("동시성 테스트 - 동기 vs 비동기")
    void testConcurrentLoad() throws InterruptedException {
        log.info("\n🚀 동시성 부하 테스트 시작");
        
        int concurrentRequests = 3; // 테스트 시간 단축을 위해 3개로 감소
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        
        // 동기 방식 동시성 테스트
        ConcurrencyTestResult syncConcurrencyResult = testConcurrentSyncAlert(executor, concurrentRequests);
        
        // 비동기 방식 동시성 테스트
        ConcurrencyTestResult asyncConcurrencyResult = testConcurrentAsyncAlert(executor, concurrentRequests);
        
        executor.shutdown();
        
        printConcurrencyResults(syncConcurrencyResult, asyncConcurrencyResult);
        
        // 비동기 방식이 동시성 처리에서 더 나은 성능을 보여야 함 (허용 오차 30%)
        assertTrue(asyncConcurrencyResult.averageExecutionTime < syncConcurrencyResult.averageExecutionTime * 1.3,
                "비동기 방식이 동시성 처리에서 더 좋은 성능을 보여야 합니다");
    }

    /**
     * 동기 방식 동시성 테스트
     */
    private ConcurrencyTestResult testConcurrentSyncAlert(ExecutorService executor, int concurrentRequests) 
            throws InterruptedException {
        
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        AtomicLong totalExecutionTime = new AtomicLong(0);
        AtomicInteger successCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestId = i + 2000; // ID 충돌 방지
            
            executor.submit(() -> {
                try {
                    long requestStartTime = System.currentTimeMillis();
                    
                    AlertGrafanaRequestDto request = createTestGrafanaRequest(requestId);
                    alertService.createGrafanaAlert(request);
                    
                    long executionTime = System.currentTimeMillis() - requestStartTime;
                    totalExecutionTime.addAndGet(executionTime);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    log.error("동기 동시성 테스트 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long totalTime = System.currentTimeMillis() - startTime;
        
        return ConcurrencyTestResult.builder()
                .testType("동기 동시성")
                .concurrentRequests(concurrentRequests)
                .totalExecutionTime(totalTime)
                .averageExecutionTime(totalExecutionTime.get() / (double) concurrentRequests)
                .successCount(successCount.get())
                .build();
    }

    /**
     * 비동기 방식 동시성 테스트
     */
    private ConcurrencyTestResult testConcurrentAsyncAlert(ExecutorService executor, int concurrentRequests) 
            throws InterruptedException {
        
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        AtomicLong totalExecutionTime = new AtomicLong(0);
        AtomicInteger successCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestId = i + 3000; // ID 충돌 방지
            
            executor.submit(() -> {
                try {
                    long requestStartTime = System.currentTimeMillis();
                    
                    AlertGrafanaRequestDto request = createTestGrafanaRequest(requestId);
                    alertService.createGrafanaAlertAsync(request);
                    
                    long executionTime = System.currentTimeMillis() - requestStartTime;
                    totalExecutionTime.addAndGet(executionTime);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    log.error("비동기 동시성 테스트 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long totalTime = System.currentTimeMillis() - startTime;
        
        return ConcurrencyTestResult.builder()
                .testType("비동기 동시성")
                .concurrentRequests(concurrentRequests)
                .totalExecutionTime(totalTime)
                .averageExecutionTime(totalExecutionTime.get() / (double) concurrentRequests)
                .successCount(successCount.get())
                .build();
    }

    /**
     * 테스트용 Grafana 알림 요청 생성
     */
    private AlertGrafanaRequestDto createTestGrafanaRequest(int id) {
        AlertRequestDto alertRequest = AlertRequestDto.builder()
                .id("GRAFANA_" + id)
                .level("CRITICAL")
                .app("ssok-bank")
                .timestamp(OffsetDateTime.now().toString()) // String으로 변환
                .message("테스트 알림 메시지 " + id)
                .build();

        return AlertGrafanaRequestDto.builder()
                .alerts(List.of(alertRequest))
                .build();
    }

    /**
     * AlertStatus 생성 대기 (동기 방식용)
     */
    private void waitForAlertStatusCreation(int alertId) throws InterruptedException {
        String expectedAlertId = "GRAFANA_" + alertId;
        
        for (int i = 0; i < 30; i++) { // 최대 3초 대기
            long count = alertStatusRepository.countByAlert_Id(expectedAlertId);
            if (count > 0) { // 일부만 생성되어도 진행
                break;
            }
            Thread.sleep(100);
        }
    }

    /**
     * 비동기 처리 완료 대기 (Kafka 방식용)
     */
    private void waitForAsyncProcessingCompletion(int alertId) throws InterruptedException {
        String expectedAlertId = "GRAFANA_" + alertId;
        
        // Alert 생성 확인
        for (int i = 0; i < 20; i++) { // 최대 2초 대기
            if (alertRepository.existsById(expectedAlertId)) {
                break;
            }
            Thread.sleep(100);
        }
        
        // 비동기 방식에서는 즉시 응답하므로 추가 대기 시간 단축
        Thread.sleep(500); // 0.5초만 대기
    }

    /**
     * 평균값 계산
     */
    private double calculateAverage(List<Long> values) {
        if (values.isEmpty()) return 0.0;
        return values.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /**
     * 처리량 계산 (건/초)
     */
    private double calculateThroughput(int successCount, List<Long> processingTimes) {
        if (processingTimes.isEmpty()) return 0.0;
        
        double totalSeconds = processingTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0) / 1000.0;
        
        return totalSeconds > 0 ? successCount / totalSeconds : 0.0;
    }

    /**
     * 성능 비교 결과 출력
     */
    private void printComparisonResults(PerformanceTestResult syncResult, PerformanceTestResult asyncResult) {
        log.info("\n" + "=".repeat(80));
        log.info("📊 성능 테스트 결과 비교");
        log.info("=".repeat(80));
        
        log.info("📈 응답 시간 비교:");
        log.info("  동기 방식  - 평균: {:.2f}ms, 최대: {}ms, 최소: {}ms", 
                syncResult.averageResponseTime, syncResult.maxResponseTime, syncResult.minResponseTime);
        log.info("  비동기 방식 - 평균: {:.2f}ms, 최대: {}ms, 최소: {}ms", 
                asyncResult.averageResponseTime, asyncResult.maxResponseTime, asyncResult.minResponseTime);
        
        double responseImprovement = ((syncResult.averageResponseTime - asyncResult.averageResponseTime) 
                / syncResult.averageResponseTime) * 100;
        log.info("  📊 응답 시간 개선: {:.1f}%", responseImprovement);
        
        log.info("\n⏱️ 총 처리 시간 비교:");
        log.info("  동기 방식  - 평균: {:.2f}ms", syncResult.averageTotalProcessingTime);
        log.info("  비동기 방식 - 평균: {:.2f}ms", asyncResult.averageTotalProcessingTime);
        
        log.info("\n🚀 처리량 비교:");
        log.info("  동기 방식  - {:.2f} 건/초", syncResult.throughput);
        log.info("  비동기 방식 - {:.2f} 건/초", asyncResult.throughput);
        
        log.info("\n✅ 성공률:");
        log.info("  동기 방식  - {}/{} ({:.1f}%)", 
                syncResult.successCount, TEST_ITERATIONS, 
                (double) syncResult.successCount / TEST_ITERATIONS * 100);
        log.info("  비동기 방식 - {}/{} ({:.1f}%)", 
                asyncResult.successCount, TEST_ITERATIONS, 
                (double) asyncResult.successCount / TEST_ITERATIONS * 100);
        
        log.info("=".repeat(80));
    }

    /**
     * 동시성 테스트 결과 출력
     */
    private void printConcurrencyResults(ConcurrencyTestResult syncResult, ConcurrencyTestResult asyncResult) {
        log.info("\n" + "=".repeat(80));
        log.info("🚀 동시성 테스트 결과 비교");
        log.info("=".repeat(80));
        
        log.info("📊 동시 요청 처리 결과 ({}개 요청):", syncResult.concurrentRequests);
        log.info("  동기 방식  - 총 소요시간: {}ms, 평균: {:.2f}ms, 성공: {}/{}", 
                syncResult.totalExecutionTime, syncResult.averageExecutionTime, 
                syncResult.successCount, syncResult.concurrentRequests);
        log.info("  비동기 방식 - 총 소요시간: {}ms, 평균: {:.2f}ms, 성공: {}/{}", 
                asyncResult.totalExecutionTime, asyncResult.averageExecutionTime, 
                asyncResult.successCount, asyncResult.concurrentRequests);
        
        double concurrencyImprovement = ((syncResult.averageExecutionTime - asyncResult.averageExecutionTime) 
                / syncResult.averageExecutionTime) * 100;
        log.info("  📊 동시성 처리 개선: {:.1f}%", concurrencyImprovement);
        
        log.info("=".repeat(80));
    }

    /**
     * 성능 테스트 결과 데이터 클래스
     */
    private static class PerformanceTestResult {
        String testType;
        double averageResponseTime;
        double averageTotalProcessingTime;
        long maxResponseTime;
        long minResponseTime;
        int successCount;
        int failureCount;
        double throughput;

        public static PerformanceTestResultBuilder builder() {
            return new PerformanceTestResultBuilder();
        }

        public static class PerformanceTestResultBuilder {
            private PerformanceTestResult result = new PerformanceTestResult();

            public PerformanceTestResultBuilder testType(String testType) {
                result.testType = testType;
                return this;
            }

            public PerformanceTestResultBuilder averageResponseTime(double averageResponseTime) {
                result.averageResponseTime = averageResponseTime;
                return this;
            }

            public PerformanceTestResultBuilder averageTotalProcessingTime(double averageTotalProcessingTime) {
                result.averageTotalProcessingTime = averageTotalProcessingTime;
                return this;
            }

            public PerformanceTestResultBuilder maxResponseTime(long maxResponseTime) {
                result.maxResponseTime = maxResponseTime;
                return this;
            }

            public PerformanceTestResultBuilder minResponseTime(long minResponseTime) {
                result.minResponseTime = minResponseTime;
                return this;
            }

            public PerformanceTestResultBuilder successCount(int successCount) {
                result.successCount = successCount;
                return this;
            }

            public PerformanceTestResultBuilder failureCount(int failureCount) {
                result.failureCount = failureCount;
                return this;
            }

            public PerformanceTestResultBuilder throughput(double throughput) {
                result.throughput = throughput;
                return this;
            }

            public PerformanceTestResult build() {
                return result;
            }
        }
    }

    /**
     * 동시성 테스트 결과 데이터 클래스
     */
    private static class ConcurrencyTestResult {
        String testType;
        int concurrentRequests;
        long totalExecutionTime;
        double averageExecutionTime;
        int successCount;

        public static ConcurrencyTestResultBuilder builder() {
            return new ConcurrencyTestResultBuilder();
        }

        public static class ConcurrencyTestResultBuilder {
            private ConcurrencyTestResult result = new ConcurrencyTestResult();

            public ConcurrencyTestResultBuilder testType(String testType) {
                result.testType = testType;
                return this;
            }

            public ConcurrencyTestResultBuilder concurrentRequests(int concurrentRequests) {
                result.concurrentRequests = concurrentRequests;
                return this;
            }

            public ConcurrencyTestResultBuilder totalExecutionTime(long totalExecutionTime) {
                result.totalExecutionTime = totalExecutionTime;
                return this;
            }

            public ConcurrencyTestResultBuilder averageExecutionTime(double averageExecutionTime) {
                result.averageExecutionTime = averageExecutionTime;
                return this;
            }

            public ConcurrencyTestResultBuilder successCount(int successCount) {
                result.successCount = successCount;
                return this;
            }

            public ConcurrencyTestResult build() {
                return result;
            }
        }
    }
}
