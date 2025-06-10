package kr.ssok.ssom.backend.domain.user.service;

import kr.ssok.ssom.backend.domain.user.service.Impl.BiometricFailureServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BiometricFailureService 테스트")
class BiometricFailureServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private BiometricFailureServiceImpl biometricFailureService;

    private static final String TEST_EMPLOYEE_ID = "CHN0001";
    private static final String TEST_DEVICE_ID = "DEVICE001";

    @Nested
    @DisplayName("실패 횟수 증가 테스트")
    class IncrementFailCountTest {

        @Test
        @DisplayName("첫 번째 실패 시 카운트가 1이 된다")
        void incrementFailCount_FirstFailure_ReturnsOne() {
            // given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                    .thenReturn(1L);

            // when
            int result = biometricFailureService.incrementFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("연속 실패 시 카운트가 누적된다")
        void incrementFailCount_ConsecutiveFailures_IncrementsCount() {
            // given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                    .thenReturn(2L);

            // when
            int result = biometricFailureService.incrementFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isEqualTo(2);
        }

        @Test
        @DisplayName("사원번호가 null일 때 예외가 발생한다")
        void incrementFailCount_NullEmployeeId_ThrowsException() {
            // when & then
            assertThatThrownBy(() ->
                    biometricFailureService.incrementFailCount(null, TEST_DEVICE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사원번호는 필수입니다");
        }

        @Test
        @DisplayName("디바이스 ID가 null일 때 예외가 발생한다")
        void incrementFailCount_NullDeviceId_ThrowsException() {
            // when & then
            assertThatThrownBy(() ->
                    biometricFailureService.incrementFailCount(TEST_EMPLOYEE_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("디바이스 ID는 필수입니다");
        }

        @Test
        @DisplayName("빈 문자열 입력 시 예외가 발생한다")
        void incrementFailCount_EmptyString_ThrowsException() {
            // when & then
            assertThatThrownBy(() ->
                    biometricFailureService.incrementFailCount("", TEST_DEVICE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Redis 오류 시 기본값 1을 반환한다")
        void incrementFailCount_RedisError_ReturnsDefaultValue() {
            // given
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                    .thenThrow(new RuntimeException("Redis connection error"));

            // when
            int result = biometricFailureService.incrementFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("실패 횟수 조회 테스트")
    class GetFailCountTest {

        @Test
        @DisplayName("실패 기록이 없으면 0을 반환한다")
        void getFailCount_NoRecord_ReturnsZero() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            // when
            int result = biometricFailureService.getFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("실패 기록이 있으면 해당 값을 반환한다")
        void getFailCount_HasRecord_ReturnsCount() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn("2");

            // when
            int result = biometricFailureService.getFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isEqualTo(2);
        }

        @Test
        @DisplayName("잘못된 형식의 값이 저장된 경우 0을 반환하고 키를 삭제한다")
        void getFailCount_InvalidFormat_ReturnsZeroAndClearsKey() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn("invalid_number");

            // when
            int result = biometricFailureService.getFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isEqualTo(0);
            verify(redisTemplate).delete(anyString());
        }
    }

    @Nested
    @DisplayName("실패 횟수 초기화 테스트")
    class ClearFailCountTest {

        @Test
        @DisplayName("실패 횟수를 성공적으로 초기화한다")
        void clearFailCount_Success() {
            // when
            biometricFailureService.clearFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            verify(redisTemplate).delete(anyString());
        }
    }

    @Nested
    @DisplayName("최대 시도 횟수 초과 확인 테스트")
    class HasExceededMaxAttemptsTest {

        @Test
        @DisplayName("실패 횟수가 3 미만이면 false를 반환한다")
        void hasExceededMaxAttempts_LessThanThree_ReturnsFalse() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn("2");

            // when
            boolean result = biometricFailureService.hasExceededMaxAttempts(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("실패 횟수가 3 이상이면 true를 반환한다")
        void hasExceededMaxAttempts_ThreeOrMore_ReturnsTrue() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn("3");

            // when
            boolean result = biometricFailureService.hasExceededMaxAttempts(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("실패 기록이 없으면 false를 반환한다")
        void hasExceededMaxAttempts_NoRecord_ReturnsFalse() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            // when
            boolean result = biometricFailureService.hasExceededMaxAttempts(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("디바이스 차단 테스트")
    class BlockDeviceTest {

        @Test
        @DisplayName("디바이스를 성공적으로 차단한다")
        void blockDevice_Success() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            biometricFailureService.blockDevice(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            verify(valueOperations).set(anyString(), eq("blocked"), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("디바이스 차단 상태 확인 테스트")
    class IsDeviceBlockedTest {

        @Test
        @DisplayName("차단된 디바이스는 true를 반환한다")
        void isDeviceBlocked_BlockedDevice_ReturnsTrue() {
            // given
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            // when
            boolean result = biometricFailureService.isDeviceBlocked(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("차단되지 않은 디바이스는 false를 반환한다")
        void isDeviceBlocked_NotBlockedDevice_ReturnsFalse() {
            // given
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // when
            boolean result = biometricFailureService.isDeviceBlocked(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("디바이스 차단 해제 테스트")
    class UnblockDeviceTest {

        @Test
        @DisplayName("디바이스 차단을 성공적으로 해제한다")
        void unblockDevice_Success() {
            // when
            biometricFailureService.unblockDevice(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            verify(redisTemplate, times(2)).delete(anyString());
        }
    }

    @Nested
    @DisplayName("남은 시도 횟수 테스트")
    class GetRemainingAttemptsTest {

        @Test
        @DisplayName("실패 횟수에 따른 남은 시도 횟수를 정확히 계산한다")
        void getRemainingAttempts_CalculatesCorrectly() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn("1");

            // when
            int result = biometricFailureService.getRemainingAttempts(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isEqualTo(2); // 3 - 1 = 2
        }

        @Test
        @DisplayName("실패 횟수가 최대값을 초과해도 0을 반환한다")
        void getRemainingAttempts_ExceedsMax_ReturnsZero() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn("5");

            // when
            int result = biometricFailureService.getRemainingAttempts(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("실패 기록이 없으면 최대 시도 횟수를 반환한다")
        void getRemainingAttempts_NoFailures_ReturnsMaxAttempts() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            // when
            int result = biometricFailureService.getRemainingAttempts(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // then
            assertThat(result).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("사용자 모든 디바이스 차단 해제 테스트")
    class UnblockAllDevicesForUserTest {

        @Test
        @DisplayName("Set에 저장된 디바이스들을 모두 해제한다")
        void unblockAllDevicesForUser_WithDevicesInSet_UnblocksAll() {
            // given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            Set<String> deviceIds = new HashSet<>(Arrays.asList("DEVICE001", "DEVICE002", "DEVICE003"));
            when(setOperations.members(anyString())).thenReturn(deviceIds);

            // when
            biometricFailureService.unblockAllDevicesForUser(TEST_EMPLOYEE_ID);

            // then
            verify(redisTemplate, atLeast(1)).delete(anyString());
        }

        @Test
        @DisplayName("Set이 비어있으면 fallback 메서드를 사용한다")
        void unblockAllDevicesForUser_EmptySet_UsesFallback() {
            // given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.members(anyString())).thenReturn(new HashSet<>());

            Set<String> failKeys = new HashSet<>(Arrays.asList("biometric:fail:" + TEST_EMPLOYEE_ID + ":DEVICE001"));
            Set<String> blockKeys = new HashSet<>(Arrays.asList("biometric:block:" + TEST_EMPLOYEE_ID + ":DEVICE001"));

            when(redisTemplate.keys(anyString())).thenReturn(failKeys, blockKeys);

            // when
            biometricFailureService.unblockAllDevicesForUser(TEST_EMPLOYEE_ID);

            // then
            verify(redisTemplate, times(2)).keys(anyString());
        }

        @Test
        @DisplayName("사원번호가 null이면 경고 로그만 남기고 종료한다")
        void unblockAllDevicesForUser_NullEmployeeId_LogsWarningAndReturns() {
            // when
            biometricFailureService.unblockAllDevicesForUser(null);

            // then - 아무 동작도 하지 않음
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("사원번호가 빈 문자열이면 경고 로그만 남기고 종료한다")
        void unblockAllDevicesForUser_EmptyEmployeeId_LogsWarningAndReturns() {
            // when
            biometricFailureService.unblockAllDevicesForUser("");

            // then - 아무 동작도 하지 않음
            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("실패 3회 후 차단되는 전체 플로우 테스트")
        void fullFailureFlow_ThreeFailuresLeadsToBlock() {
            // 새로운 mock 인스턴스들을 생성하여 테스트 격리
            RedisTemplate<String, String> isolatedRedisTemplate = mock(RedisTemplate.class);
            ValueOperations<String, String> isolatedValueOperations = mock(ValueOperations.class);
            SetOperations<String, String> isolatedSetOperations = mock(SetOperations.class);

            BiometricFailureServiceImpl isolatedService = new BiometricFailureServiceImpl(isolatedRedisTemplate);

            // given
            when(isolatedRedisTemplate.opsForValue()).thenReturn(isolatedValueOperations);
            when(isolatedRedisTemplate.opsForSet()).thenReturn(isolatedSetOperations);
            when(isolatedRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                    .thenReturn(1L, 2L, 3L);
            when(isolatedValueOperations.get(anyString()))
                    .thenReturn("1", "2", "3");

            // isDeviceBlocked 호출을 위한 hasKey mock
            when(isolatedRedisTemplate.hasKey(anyString())).thenReturn(true);

            // when & then
            // 첫 번째 실패
            int firstFail = isolatedService.incrementFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);
            assertThat(firstFail).isEqualTo(1);
            assertThat(isolatedService.hasExceededMaxAttempts(TEST_EMPLOYEE_ID, TEST_DEVICE_ID)).isFalse();

            // 두 번째 실패
            int secondFail = isolatedService.incrementFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);
            assertThat(secondFail).isEqualTo(2);
            assertThat(isolatedService.hasExceededMaxAttempts(TEST_EMPLOYEE_ID, TEST_DEVICE_ID)).isFalse();

            // 세 번째 실패 (차단 조건 충족)
            int thirdFail = isolatedService.incrementFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);
            assertThat(thirdFail).isEqualTo(3);
            assertThat(isolatedService.hasExceededMaxAttempts(TEST_EMPLOYEE_ID, TEST_DEVICE_ID)).isTrue();

            // 디바이스 차단
            isolatedService.blockDevice(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);

            // 차단 상태 확인
            assertThat(isolatedService.isDeviceBlocked(TEST_EMPLOYEE_ID, TEST_DEVICE_ID)).isTrue();
        }

        @Test
        @DisplayName("성공 후 실패 카운트 초기화 시나리오")
        void successAfterFailures_ClearsFailCount() {
            // 새로운 mock 인스턴스들을 생성하여 테스트 격리
            RedisTemplate<String, String> isolatedRedisTemplate = mock(RedisTemplate.class);
            ValueOperations<String, String> isolatedValueOperations = mock(ValueOperations.class);
            SetOperations<String, String> isolatedSetOperations = mock(SetOperations.class);

            BiometricFailureServiceImpl isolatedService = new BiometricFailureServiceImpl(isolatedRedisTemplate);

            // given
            when(isolatedRedisTemplate.opsForValue()).thenReturn(isolatedValueOperations);
            when(isolatedRedisTemplate.opsForSet()).thenReturn(isolatedSetOperations);
            when(isolatedRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                    .thenReturn(2L);
            when(isolatedValueOperations.get(anyString()))
                    .thenReturn("2")
                    .thenReturn((String) null);

            // when
            // 실패 2회
            isolatedService.incrementFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);
            assertThat(isolatedService.getFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID)).isEqualTo(2);

            // 성공으로 인한 초기화
            isolatedService.clearFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID);
            assertThat(isolatedService.getFailCount(TEST_EMPLOYEE_ID, TEST_DEVICE_ID)).isEqualTo(0);

            // then
            assertThat(isolatedService.getRemainingAttempts(TEST_EMPLOYEE_ID, TEST_DEVICE_ID)).isEqualTo(3);
        }
    }
}