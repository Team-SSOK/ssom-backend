package kr.ssok.ssom.backend.domain.alert.performance;

import kr.ssok.ssom.backend.domain.alert.dto.AlertGrafanaRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertRequestDto;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Alert 대용량 부하 테스트
 * 실제 운영 환경에서 발생할 수 있는 대량 알림 시나리오 테스트
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AlertLoadTest {

    private static final Logger log = LoggerFactory.getLogger(AlertLoadTest.class);

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertStatusRepository alertStatusRepository;

    // Firebase와 Redis를 Mock으로 처리
    @MockitoBean
    private FirebaseClient firebaseClient;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    private static final int TARGET_USER_COUNT = 200; // 테스트 시간 단축을 위해 200명으로 감소
    private static final int CONCURRENT_ALERTS = 10;  // 동시 알림 발생 수 감소
    private static final int TOTAL_ALERTS = 20;       // 총 알림 수 감소

    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // Redis Mock 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("mock-fcm-token");
        
        // Firebase Mock 설정 (아무것도 하지 않음)
        doNothing().when(firebaseClient).sendNotification(any());
        
        createLargeUserBase();
    }

    /**
     * 대규모 사용자 기반 생성 (실제 운영 환경 시뮬레이션)
     */
    private void createLargeUserBase() {
        testUsers = new ArrayList<>();
        
        for (int i = 1; i <= TARGET_USER_COUNT; i++) {
            Department dept;
            // 부서별 분포 (실제 조직 구조 반영)
            if (i % 10 == 0) dept = Department.OPERATION;      // 10%
            else if (i % 5 == 0) dept = Department.EXTERNAL;   // 10% (중복 제외)
            else if (i % 2 == 0) dept = Department.CORE_BANK;  // 40%
            else dept = Department.CHANNEL;                     // 40%
            
            User user = User.builder()
                    .id("user" + String.format("%04d", i))
                    .username("LoadTestUser" + i)
                    .password("testpass" + i)
                    .phoneNumber("010-1000-" + String.format("%04d", i))
                    .department(dept)
                    .build();
            testUsers.add(user);
        }
        
        userRepository.saveAll(testUsers);
        log.info("대규모 테스트용 사용자 {}명 생성 완료", testUsers.size());
    }

    @Test
    @DisplayName("대용량 동시 알림 발생 시나리오 - 동기 vs 비동기")
    void testHighVolumeAlertScenario() throws InterruptedException {
        log.info("\n🔥 대용량 알림 부하 테스트 시작");
        log.info("총 사용자 수: {}명, 동시 알림 수: {}개, 총 알림 수: {}개", 
                TARGET_USER_COUNT, CONCURRENT_ALERTS, TOTAL_ALERTS);

        // 동기 방식 부하 테스트
        LoadTestResult syncResult = runLoadTest("동기", false);
        
        // 잠시 대기 (시스템 안정화)
        Thread.sleep(2000);
        
        // 비동기 방식 부하 테스트  
        LoadTestResult asyncResult = runLoadTest("비동기", true);
        
        printLoadTestResults(syncResult, asyncResult);
        
        // 성능 검증
        assertLoadTestResults(syncResult, asyncResult);
    }

    /**
     * 부하 테스트 실행
     */
    private LoadTestResult runLoadTest(String testName, boolean useAsync) throws InterruptedException {
        log.info("\n📊 {} 방식 부하 테스트 시작", testName);
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ALERTS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(TOTAL_ALERTS);
        
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicLong maxResponseTime = new AtomicLong(0);
        AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long testStartTime = System.currentTimeMillis();
        
        // 모든 요청을 동시에 시작할 수 있도록 준비
        for (int i = 0; i < TOTAL_ALERTS; i++) {
            final int alertId = i + (useAsync ? 10000 : 5000); // ID 충돌 방지
            
            executor.submit(() -> {
                try {
                    // 모든 스레드가 동시에 시작하도록 대기
                    startLatch.await();
                    
                    long requestStart = System.currentTimeMillis();
                    
                    // 알림 처리
                    AlertGrafanaRequestDto request = createLoadTestRequest(alertId);
                    
                    if (useAsync) {
                        alertService.createGrafanaAlertAsync(request);
                    } else {
                        alertService.createGrafanaAlert(request);
                    }
                    
                    long responseTime = System.currentTimeMillis() - requestStart;
                    
                    // 통계 업데이트
                    totalResponseTime.addAndGet(responseTime);
                    updateMaxMin(maxResponseTime, minResponseTime, responseTime);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    log.error("{} 방식 부하 테스트 중 오류: {}", testName, e.getMessage());
                    errorCount.incrementAndGet();
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        // 모든 요청 동시 시작
        startLatch.countDown();
        
        // 모든 요청 완료 대기
        boolean completed = completeLatch.await(180, TimeUnit.SECONDS); // 최대 3분 대기
        long totalTestTime = System.currentTimeMillis() - testStartTime;
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        if (!completed) {
            log.warn("{} 방식 테스트가 제한 시간 내에 완료되지 않았습니다.", testName);
        }
        
        return LoadTestResult.builder()
                .testName(testName)
                .totalRequests(TOTAL_ALERTS)
                .successCount(successCount.get())
                .errorCount(errorCount.get())
                .totalTestTime(totalTestTime)
                .averageResponseTime(successCount.get() > 0 ? 
                    (double) totalResponseTime.get() / successCount.get() : 0)
                .maxResponseTime(maxResponseTime.get())
                .minResponseTime(minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get())
                .throughput((double) successCount.get() / (totalTestTime / 1000.0))
                .build();
    }

    /**
     * 메모리 사용량 테스트
     */
    @Test
    @DisplayName("메모리 사용량 비교 테스트")
    void testMemoryUsage() throws InterruptedException {
        log.info("\n💾 메모리 사용량 비교 테스트 시작");
        
        Runtime runtime = Runtime.getRuntime();
        
        // 동기 방식 메모리 테스트
        MemoryTestResult syncMemoryResult = runMemoryTest("동기", false, runtime);
        
        // GC 실행 및 대기
        System.gc();
        Thread.sleep(2000);
        
        // 비동기 방식 메모리 테스트
        MemoryTestResult asyncMemoryResult = runMemoryTest("비동기", true, runtime);
        
        printMemoryTestResults(syncMemoryResult, asyncMemoryResult);
    }

    /**
     * 메모리 사용량 테스트 실행
     */
    private MemoryTestResult runMemoryTest(String testName, boolean useAsync, Runtime runtime) 
            throws InterruptedException {
        
        // 초기 메모리 상태
        System.gc();
        Thread.sleep(1000);
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        log.info("{} 방식 메모리 테스트 시작 - 초기 메모리: {} MB", 
                testName, beforeMemory / 1024 / 1024);
        
        // 부하 생성
        int testAlerts = 10; // 테스트 알림 수 감소
        for (int i = 0; i < testAlerts; i++) {
            AlertGrafanaRequestDto request = createLoadTestRequest(i + (useAsync ? 20000 : 15000));
            
            if (useAsync) {
                alertService.createGrafanaAlertAsync(request);
            } else {
                alertService.createGrafanaAlert(request);
            }
            
            if (i % 5 == 0) {
                Thread.sleep(200); // 중간 대기
            }
        }
        
        // 처리 완료 대기
        Thread.sleep(3000);
        
        // 최종 메모리 상태
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        
        log.info("{} 방식 메모리 테스트 완료 - 최종 메모리: {} MB, 사용량: {} MB", 
                testName, afterMemory / 1024 / 1024, memoryUsed / 1024 / 1024);
        
        return MemoryTestResult.builder()
                .testName(testName)
                .beforeMemory(beforeMemory)
                .afterMemory(afterMemory)
                .memoryUsed(memoryUsed)
                .build();
    }

    /**
     * 스레드 풀 포화 상태 테스트
     */
    @Test
    @DisplayName("스레드 풀 포화 상태 테스트")
    void testThreadPoolSaturation() throws InterruptedException {
        log.info("\n🧵 스레드 풀 포화 상태 테스트 시작");
        
        int heavyLoad = 50; // 대량 요청 수 감소
        ExecutorService limitedExecutor = Executors.newFixedThreadPool(5); // 제한된 스레드 풀
        
        AtomicInteger syncRejections = new AtomicInteger(0);
        AtomicInteger asyncRejections = new AtomicInteger(0);
        
        // 동기 방식 포화 테스트
        testThreadPoolSaturationSync(limitedExecutor, heavyLoad, syncRejections);
        
        Thread.sleep(2000);
        
        // 비동기 방식 포화 테스트
        testThreadPoolSaturationAsync(limitedExecutor, heavyLoad, asyncRejections);
        
        limitedExecutor.shutdown();
        
        log.info("\n🧵 스레드 풀 포화 테스트 결과:");
        log.info("동기 방식 거절된 요청: {} / {}", syncRejections.get(), heavyLoad);
        log.info("비동기 방식 거절된 요청: {} / {}", asyncRejections.get(), heavyLoad);
    }

    private void testThreadPoolSaturationSync(ExecutorService executor, int requests, 
                                              AtomicInteger rejections) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(requests);
        
        for (int i = 0; i < requests; i++) {
            final int alertId = i + 30000;
            try {
                executor.submit(() -> {
                    try {
                        AlertGrafanaRequestDto request = createLoadTestRequest(alertId);
                        alertService.createGrafanaAlert(request);
                    } catch (Exception e) {
                        rejections.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            } catch (RejectedExecutionException e) {
                rejections.incrementAndGet();
                latch.countDown();
            }
        }
        
        latch.await(30, TimeUnit.SECONDS);
    }

    private void testThreadPoolSaturationAsync(ExecutorService executor, int requests, 
                                               AtomicInteger rejections) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(requests);
        
        for (int i = 0; i < requests; i++) {
            final int alertId = i + 40000;
            try {
                executor.submit(() -> {
                    try {
                        AlertGrafanaRequestDto request = createLoadTestRequest(alertId);
                        alertService.createGrafanaAlertAsync(request);
                    } catch (Exception e) {
                        rejections.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            } catch (RejectedExecutionException e) {
                rejections.incrementAndGet();
                latch.countDown();
            }
        }
        
        latch.await(30, TimeUnit.SECONDS);
    }

    private void updateMaxMin(AtomicLong max, AtomicLong min, long value) {
        max.updateAndGet(current -> Math.max(current, value));
        min.updateAndGet(current -> Math.min(current, value));
    }

    private AlertGrafanaRequestDto createLoadTestRequest(int id) {
        AlertRequestDto alertRequest = AlertRequestDto.builder()
                .id("LOAD_TEST_" + id)
                .level("HIGH")
                .app("ssok-bank")
                .timestamp(OffsetDateTime.now().toString()) // String으로 변환
                .message("부하 테스트 알림 메시지 " + id)
                .build();

        return AlertGrafanaRequestDto.builder()
                .alerts(List.of(alertRequest))
                .build();
    }

    private void printLoadTestResults(LoadTestResult syncResult, LoadTestResult asyncResult) {
        log.info("\n" + "=".repeat(100));
        log.info("🔥 대용량 부하 테스트 결과 비교");
        log.info("=".repeat(100));
        
        printSingleLoadTestResult(syncResult);
        printSingleLoadTestResult(asyncResult);
        
        // 개선율 계산
        double responseTimeImprovement = 0;
        double throughputImprovement = 0;
        
        if (syncResult.averageResponseTime > 0) {
            responseTimeImprovement = ((syncResult.averageResponseTime - asyncResult.averageResponseTime) 
                    / syncResult.averageResponseTime) * 100;
        }
        
        if (syncResult.throughput > 0) {
            throughputImprovement = ((asyncResult.throughput - syncResult.throughput) 
                    / syncResult.throughput) * 100;
        }
        
        log.info("\n📊 성능 개선 요약:");
        log.info("  응답 시간 개선: {:.1f}%", responseTimeImprovement);
        log.info("  처리량 개선: {:.1f}%", throughputImprovement);
        log.info("  에러율 비교: 동기 {:.1f}% vs 비동기 {:.1f}%", 
                (double) syncResult.errorCount / syncResult.totalRequests * 100,
                (double) asyncResult.errorCount / asyncResult.totalRequests * 100);
        
        log.info("=".repeat(100));
    }

    private void printSingleLoadTestResult(LoadTestResult result) {
        log.info("\n📊 {} 방식 결과:", result.testName);
        log.info("  총 요청: {} / 성공: {} / 실패: {}", 
                result.totalRequests, result.successCount, result.errorCount);
        log.info("  총 소요시간: {:.2f}초", result.totalTestTime / 1000.0);
        log.info("  평균 응답시간: {:.2f}ms", result.averageResponseTime);
        log.info("  최대 응답시간: {}ms / 최소 응답시간: {}ms", 
                result.maxResponseTime, result.minResponseTime);
        log.info("  처리량: {:.2f} 요청/초", result.throughput);
        log.info("  성공률: {:.1f}%", (double) result.successCount / result.totalRequests * 100);
    }

    private void printMemoryTestResults(MemoryTestResult syncResult, MemoryTestResult asyncResult) {
        log.info("\n" + "=".repeat(80));
        log.info("💾 메모리 사용량 테스트 결과");
        log.info("=".repeat(80));
        
        log.info("동기 방식:");
        log.info("  사용 전: {} MB / 사용 후: {} MB / 증가량: {} MB", 
                syncResult.beforeMemory / 1024 / 1024,
                syncResult.afterMemory / 1024 / 1024,
                syncResult.memoryUsed / 1024 / 1024);
        
        log.info("비동기 방식:");
        log.info("  사용 전: {} MB / 사용 후: {} MB / 증가량: {} MB", 
                asyncResult.beforeMemory / 1024 / 1024,
                asyncResult.afterMemory / 1024 / 1024,
                asyncResult.memoryUsed / 1024 / 1024);
        
        if (syncResult.memoryUsed > 0) {
            double memoryImprovement = ((double) syncResult.memoryUsed - asyncResult.memoryUsed) 
                    / syncResult.memoryUsed * 100;
            log.info("메모리 사용량 개선: {:.1f}%", memoryImprovement);
        }
        
        log.info("=".repeat(80));
    }

    private void assertLoadTestResults(LoadTestResult syncResult, LoadTestResult asyncResult) {
        // 기본적인 성능 검증 (관대한 조건으로 설정)
        assertTrue(asyncResult.averageResponseTime < syncResult.averageResponseTime * 2.0, 
                "비동기 방식의 응답시간이 예상보다 매우 느립니다");
        
        assertTrue(asyncResult.successCount >= syncResult.successCount * 0.8, 
                "비동기 방식의 성공률이 예상보다 낮습니다");
        
        assertTrue(asyncResult.errorCount <= syncResult.errorCount * 1.5, 
                "비동기 방식의 에러 발생률이 예상보다 높습니다");
    }

    /**
     * 부하 테스트 결과 데이터 클래스
     */
    private static class LoadTestResult {
        String testName;
        int totalRequests;
        int successCount;
        int errorCount;
        long totalTestTime;
        double averageResponseTime;
        long maxResponseTime;
        long minResponseTime;
        double throughput;

        public static LoadTestResultBuilder builder() {
            return new LoadTestResultBuilder();
        }

        public static class LoadTestResultBuilder {
            private LoadTestResult result = new LoadTestResult();

            public LoadTestResultBuilder testName(String testName) {
                result.testName = testName;
                return this;
            }

            public LoadTestResultBuilder totalRequests(int totalRequests) {
                result.totalRequests = totalRequests;
                return this;
            }

            public LoadTestResultBuilder successCount(int successCount) {
                result.successCount = successCount;
                return this;
            }

            public LoadTestResultBuilder errorCount(int errorCount) {
                result.errorCount = errorCount;
                return this;
            }

            public LoadTestResultBuilder totalTestTime(long totalTestTime) {
                result.totalTestTime = totalTestTime;
                return this;
            }

            public LoadTestResultBuilder averageResponseTime(double averageResponseTime) {
                result.averageResponseTime = averageResponseTime;
                return this;
            }

            public LoadTestResultBuilder maxResponseTime(long maxResponseTime) {
                result.maxResponseTime = maxResponseTime;
                return this;
            }

            public LoadTestResultBuilder minResponseTime(long minResponseTime) {
                result.minResponseTime = minResponseTime;
                return this;
            }

            public LoadTestResultBuilder throughput(double throughput) {
                result.throughput = throughput;
                return this;
            }

            public LoadTestResult build() {
                return result;
            }
        }
    }

    /**
     * 메모리 테스트 결과 데이터 클래스
     */
    private static class MemoryTestResult {
        String testName;
        long beforeMemory;
        long afterMemory;
        long memoryUsed;

        public static MemoryTestResultBuilder builder() {
            return new MemoryTestResultBuilder();
        }

        public static class MemoryTestResultBuilder {
            private MemoryTestResult result = new MemoryTestResult();

            public MemoryTestResultBuilder testName(String testName) {
                result.testName = testName;
                return this;
            }

            public MemoryTestResultBuilder beforeMemory(long beforeMemory) {
                result.beforeMemory = beforeMemory;
                return this;
            }

            public MemoryTestResultBuilder afterMemory(long afterMemory) {
                result.afterMemory = afterMemory;
                return this;
            }

            public MemoryTestResultBuilder memoryUsed(long memoryUsed) {
                result.memoryUsed = memoryUsed;
                return this;
            }

            public MemoryTestResult build() {
                return result;
            }
        }
    }
}
