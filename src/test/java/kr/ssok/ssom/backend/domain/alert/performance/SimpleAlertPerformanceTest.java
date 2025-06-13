package kr.ssok.ssom.backend.domain.alert.performance;

import kr.ssok.ssom.backend.domain.alert.dto.AlertGrafanaRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertRequestDto;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * 간단한 Alert 성능 테스트
 * 기존 동기 방식 vs Kafka 비동기 방식 기본 성능 비교
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SimpleAlertPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(SimpleAlertPerformanceTest.class);

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserRepository userRepository;

    // 외부 의존성 Mock 처리
    @MockitoBean
    private FirebaseClient firebaseClient;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    private static final int TEST_USER_COUNT = 10; // 간단한 테스트를 위해 10명으로 설정
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // Mock 설정
        setupMocks();

        // 테스트용 사용자 생성
        createTestUsers();
    }

    private void setupMocks() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("mock-fcm-token");
        doNothing().when(firebaseClient).sendNotification(any());
    }

    private void createTestUsers() {
        testUsers = new ArrayList<>();

        for (int i = 1; i <= TEST_USER_COUNT; i++) {
            User user = User.builder()
                    .id("test" + String.format("%03d", i))
                    .username("TestUser" + i)
                    .password("password" + i)
                    .phoneNumber("010-0000-" + String.format("%04d", i))
                    .department(i % 2 == 0 ? Department.OPERATION : Department.CORE_BANK)
                    .build();
            testUsers.add(user);
        }

        userRepository.saveAll(testUsers);
        log.info("테스트용 사용자 {}명 생성 완료", testUsers.size());
    }

    @Test
    @DisplayName("간단한 동기 vs 비동기 방식 응답시간 비교")
    void compareBasicPerformance() {
        log.info("=== 간단한 성능 테스트 시작 ===");
        log.info("대상 사용자 수: {}명", TEST_USER_COUNT);

        // 동기 방식 테스트
        long syncTime = measureSyncPerformance();

        // 비동기 방식 테스트
        long asyncTime = measureAsyncPerformance();

        // 결과 출력
        printResults(syncTime, asyncTime);

        // 비동기 방식이 더 빨라야 함 (허용 오차 50%)
        assertTrue(asyncTime <= syncTime * 1.5,
                String.format("비동기 방식(%dms)이 동기 방식(%dms)보다 빠르거나 비슷해야 합니다", asyncTime, syncTime));
    }

    private long measureSyncPerformance() {
        log.info("동기 방식 성능 측정 시작");

        long startTime = System.currentTimeMillis();

        try {
            AlertGrafanaRequestDto request = createTestRequest("SYNC_TEST");
            alertService.createGrafanaAlert(request);

            long responseTime = System.currentTimeMillis() - startTime;
            log.info("동기 방식 완료 - 응답시간: {}ms", responseTime);
            return responseTime;

        } catch (Exception e) {
            log.error("동기 방식 테스트 실패: {}", e.getMessage());
            return -1;
        }
    }

    private long measureAsyncPerformance() {
        log.info("비동기 방식 성능 측정 시작");

        long startTime = System.currentTimeMillis();

        try {
            AlertGrafanaRequestDto request = createTestRequest("ASYNC_TEST");
            alertService.createGrafanaAlertAsync(request);

            long responseTime = System.currentTimeMillis() - startTime;
            log.info("비동기 방식 완료 - 응답시간: {}ms", responseTime);
            return responseTime;

        } catch (Exception e) {
            log.error("비동기 방식 테스트 실패: {}", e.getMessage());
            return -1;
        }
    }

    private AlertGrafanaRequestDto createTestRequest(String id) {
        AlertRequestDto alertRequest = AlertRequestDto.builder()
                .id(id)
                .level("CRITICAL")
                .app("ssok-bank")
                .timestamp(OffsetDateTime.now().toString())
                .message("성능 테스트 알림 메시지")
                .build();

        return AlertGrafanaRequestDto.builder()
                .alerts(List.of(alertRequest))
                .build();
    }

    private void printResults(long syncTime, long asyncTime) {
        log.info("\n" + "=".repeat(60));
        log.info("📊 간단한 성능 테스트 결과");
        log.info("=".repeat(60));
        log.info("동기 방식 응답시간:   {}ms", syncTime);
        log.info("비동기 방식 응답시간: {}ms", asyncTime);

        if (syncTime > 0 && asyncTime > 0) {
            double improvement = ((double) (syncTime - asyncTime) / syncTime) * 100;
            log.info("응답시간 개선:       {:.1f}%", improvement);

            if (asyncTime < syncTime) {
                log.info("✅ 비동기 방식이 {}ms 더 빠릅니다!", syncTime - asyncTime);
            } else if (asyncTime > syncTime) {
                log.info("⚠️ 동기 방식이 {}ms 더 빠릅니다", asyncTime - syncTime);
            } else {
                log.info("⚡ 두 방식의 성능이 동일합니다");
            }
        }
        log.info("=".repeat(60));
    }

    @Test
    @DisplayName("여러 번 실행하여 평균 성능 측정")
    void measureAveragePerformance() {
        log.info("=== 평균 성능 측정 테스트 시작 ===");

        int iterations = 5;
        List<Long> syncTimes = new ArrayList<>();
        List<Long> asyncTimes = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            log.info("{}회차 테스트 진행 중...", i + 1);

            // 동기 방식
            long syncTime = measureSyncPerformance();
            if (syncTime > 0) {
                syncTimes.add(syncTime);
            }

            // 잠시 대기
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 비동기 방식
            long asyncTime = measureAsyncPerformance();
            if (asyncTime > 0) {
                asyncTimes.add(asyncTime);
            }

            // 테스트 간 간격
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        printAverageResults(syncTimes, asyncTimes);
    }

    private void printAverageResults(List<Long> syncTimes, List<Long> asyncTimes) {
        if (syncTimes.isEmpty() || asyncTimes.isEmpty()) {
            log.warn("측정된 데이터가 부족합니다.");
            return;
        }

        double avgSync = syncTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgAsync = asyncTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        log.info("\n" + "=".repeat(70));
        log.info("📊 평균 성능 테스트 결과 ({}회 측정)", syncTimes.size());
        log.info("=".repeat(70));
        log.info("동기 방식 평균:   {:.1f}ms", avgSync);
        log.info("비동기 방식 평균: {:.1f}ms", avgAsync);

        if (avgSync > 0 && avgAsync > 0) {
            double improvement = ((avgSync - avgAsync) / avgSync) * 100;
            log.info("평균 응답시간 개선: {:.1f}%", improvement);
        }
        log.info("=".repeat(70));
    }
}
