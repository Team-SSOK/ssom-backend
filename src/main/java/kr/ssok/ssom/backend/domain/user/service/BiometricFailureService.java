package kr.ssok.ssom.backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiometricFailureService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BIOMETRIC_FAIL_KEY = "biometric:fail:";
    private static final String BIOMETRIC_BLOCK_KEY = "biometric:block:";
    private static final Duration FAIL_COUNT_TTL = Duration.ofMinutes(30);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(30);
    private static final int MAX_ATTEMPTS = 3;

    /**
     * 실패 횟수 증가
     */
    public int incrementFailCount(String employeeId, String deviceId) {
        String key = BIOMETRIC_FAIL_KEY + employeeId + ":" + deviceId;
        String count = redisTemplate.opsForValue().get(key);

        int newCount = (count != null ? Integer.parseInt(count) : 0) + 1;
        redisTemplate.opsForValue().set(key, String.valueOf(newCount), FAIL_COUNT_TTL);

        log.debug("생체인증 실패 횟수 증가 - 사원번호: {}, 디바이스: {}, 횟수: {}", employeeId, deviceId, newCount);

        return newCount;
    }

    /**
     * 실패 횟수 초기화
     */
    public void clearFailCount(String employeeId, String deviceId) {
        String key = BIOMETRIC_FAIL_KEY + employeeId + ":" + deviceId;
        redisTemplate.delete(key);
        log.debug("생체인증 실패 횟수 초기화 - 사원번호: {}, 디바이스: {}", employeeId, deviceId);
    }

    /**
     * 현재 실패 횟수 조회
     */
    public int getFailCount(String employeeId, String deviceId) {
        String key = BIOMETRIC_FAIL_KEY + employeeId + ":" + deviceId;
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * 최대 시도 횟수 초과 여부 확인
     */
    public boolean hasExceededMaxAttempts(String employeeId, String deviceId) {
        return getFailCount(employeeId, deviceId) >= MAX_ATTEMPTS;
    }

    /**
     * 디바이스 차단
     */
    public void blockDevice(String employeeId, String deviceId) {
        String key = BIOMETRIC_BLOCK_KEY + employeeId + ":" + deviceId;
        redisTemplate.opsForValue().set(key, "blocked", BLOCK_DURATION);
        log.warn("디바이스 차단됨 - 사원번호: {}, 디바이스: {}, 기간: {}분", 
                employeeId, deviceId, BLOCK_DURATION.toMinutes());
    }

    /**
     * 디바이스 차단 상태 확인
     */
    public boolean isDeviceBlocked(String employeeId, String deviceId) {
        String key = BIOMETRIC_BLOCK_KEY + employeeId + ":" + deviceId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 디바이스 차단 해제
     */
    public void unblockDevice(String employeeId, String deviceId) {
        String failKey = BIOMETRIC_FAIL_KEY + employeeId + ":" + deviceId;
        String blockKey = BIOMETRIC_BLOCK_KEY + employeeId + ":" + deviceId;
        
        redisTemplate.delete(failKey);
        redisTemplate.delete(blockKey);
        
        log.info("디바이스 차단 해제 - 사원번호: {}, 디바이스: {}", employeeId, deviceId);
    }

    /**
     * 남은 시도 횟수 반환
     */
    public int getRemainingAttempts(String employeeId, String deviceId) {
        int failCount = getFailCount(employeeId, deviceId);
        return Math.max(0, MAX_ATTEMPTS - failCount);
    }
}