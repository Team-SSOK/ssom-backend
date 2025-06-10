package kr.ssok.ssom.backend.domain.user.service.Impl;

import kr.ssok.ssom.backend.domain.user.service.BiometricFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiometricFailureServiceImpl implements BiometricFailureService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BIOMETRIC_FAIL_KEY = "biometric:fail:";
    private static final String BIOMETRIC_BLOCK_KEY = "biometric:block:";
    private static final String USER_DEVICES_KEY = "biometric:devices:";
    private static final Duration FAIL_COUNT_TTL = Duration.ofMinutes(30);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(30);
    private static final int MAX_ATTEMPTS = 3;

    // Lua 스크립트로 원자적 increment 연산
    private static final String INCREMENT_SCRIPT = 
        "local key = KEYS[1]\n" +
        "local ttl = tonumber(ARGV[1])\n" +
        "local current = redis.call('GET', key)\n" +
        "local newValue = (current and tonumber(current) or 0) + 1\n" +
        "redis.call('SET', key, newValue, 'EX', ttl)\n" +
        "return newValue";

    /**
     * 실패 횟수 증가 (원자적 연산)
     */
    @Override
    public int incrementFailCount(String employeeId, String deviceId) {
        validateInput(employeeId, deviceId);
        
        String key = BIOMETRIC_FAIL_KEY + employeeId + ":" + deviceId;
        String devicesKey = USER_DEVICES_KEY + employeeId;
        
        try {
            // 원자적으로 실패 횟수 증가
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCREMENT_SCRIPT, Long.class);
            Long newCount = redisTemplate.execute(script, 
                Arrays.asList(key), 
                String.valueOf(FAIL_COUNT_TTL.getSeconds()));
            
            // 사용자의 디바이스 목록에 추가 (차후 일괄 해제용)
            redisTemplate.opsForSet().add(devicesKey, deviceId);
            redisTemplate.expire(devicesKey, Duration.ofHours(24)); // 24시간 유지
            
            int count = newCount != null ? newCount.intValue() : 1;
            log.debug("생체인증 실패 횟수 증가 - 사원번호: {}, 디바이스: {}, 횟수: {}", employeeId, deviceId, count);
            
            return count;
        } catch (Exception e) {
            log.error("실패 횟수 증가 중 오류 발생 - 사원번호: {}, 디바이스: {}", employeeId, deviceId, e);
            return 1; // 기본값 반환
        }
    }

    /**
     * 실패 횟수 초기화
     */
    @Override
    public void clearFailCount(String employeeId, String deviceId) {
        validateInput(employeeId, deviceId);
        
        String key = BIOMETRIC_FAIL_KEY + employeeId + ":" + deviceId;
        redisTemplate.delete(key);
        log.debug("생체인증 실패 횟수 초기화 - 사원번호: {}, 디바이스: {}", employeeId, deviceId);
    }

    /**
     * 현재 실패 횟수 조회
     */
    @Override
    public int getFailCount(String employeeId, String deviceId) {
        validateInput(employeeId, deviceId);
        
        String key = BIOMETRIC_FAIL_KEY + employeeId + ":" + deviceId;
        try {
            String count = redisTemplate.opsForValue().get(key);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (NumberFormatException e) {
            log.warn("실패 횟수 파싱 오류 - 사원번호: {}, 디바이스: {}, 값: {}", employeeId, deviceId, 
                redisTemplate.opsForValue().get(key), e);
            // 잘못된 값이 저장된 경우 초기화
            clearFailCount(employeeId, deviceId);
            return 0;
        }
    }

    /**
     * 최대 시도 횟수 초과 여부 확인
     */
    @Override
    public boolean hasExceededMaxAttempts(String employeeId, String deviceId) {
        return getFailCount(employeeId, deviceId) >= MAX_ATTEMPTS;
    }

    /**
     * 디바이스 차단
     */
    @Override
    public void blockDevice(String employeeId, String deviceId) {
        validateInput(employeeId, deviceId);
        
        String key = BIOMETRIC_BLOCK_KEY + employeeId + ":" + deviceId;
        redisTemplate.opsForValue().set(key, "blocked", BLOCK_DURATION);
        log.warn("디바이스 차단됨 - 사원번호: {}, 디바이스: {}, 기간: {}분", 
                employeeId, deviceId, BLOCK_DURATION.toMinutes());
    }

    /**
     * 디바이스 차단 상태 확인
     */
    @Override
    public boolean isDeviceBlocked(String employeeId, String deviceId) {
        validateInput(employeeId, deviceId);
        
        String key = BIOMETRIC_BLOCK_KEY + employeeId + ":" + deviceId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 디바이스 차단 해제
     */
    @Override
    public void unblockDevice(String employeeId, String deviceId) {
        validateInput(employeeId, deviceId);
        
        String failKey = BIOMETRIC_FAIL_KEY + employeeId + ":" + deviceId;
        String blockKey = BIOMETRIC_BLOCK_KEY + employeeId + ":" + deviceId;
        
        redisTemplate.delete(failKey);
        redisTemplate.delete(blockKey);
        
        log.info("디바이스 차단 해제 - 사원번호: {}, 디바이스: {}", employeeId, deviceId);
    }

    /**
     * 남은 시도 횟수 반환
     */
    @Override
    public int getRemainingAttempts(String employeeId, String deviceId) {
        int failCount = getFailCount(employeeId, deviceId);
        return Math.max(0, MAX_ATTEMPTS - failCount);
    }

    /**
     * 사용자의 모든 디바이스 차단 해제 (개선된 버전)
     */
    @Override
    public void unblockAllDevicesForUser(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            log.warn("사원번호가 비어있음");
            return;
        }
        
        try {
            String devicesKey = USER_DEVICES_KEY + employeeId;
            Set<String> deviceIds = redisTemplate.opsForSet().members(devicesKey);
            
            if (deviceIds != null && !deviceIds.isEmpty()) {
                for (String deviceId : deviceIds) {
                    unblockDevice(employeeId, deviceId);
                }
                // 디바이스 목록도 삭제
                redisTemplate.delete(devicesKey);
                
                log.info("사용자의 모든 디바이스 차단 해제 완료 - 사원번호: {}, 디바이스 수: {}", 
                    employeeId, deviceIds.size());
            } else {
                // Set에 디바이스가 없는 경우 fallback으로 패턴 검색 (제한적으로 사용)
                fallbackUnblockAllDevices(employeeId);
            }
        } catch (Exception e) {
            log.error("모든 디바이스 차단 해제 중 오류 발생 - 사원번호: {}", employeeId, e);
            // 오류 발생 시 fallback 메서드 사용
            fallbackUnblockAllDevices(employeeId);
        }
    }

    /**
     * 패턴 검색을 이용한 fallback 메서드
     */
    private void fallbackUnblockAllDevices(String employeeId) {
        try {
            String failPattern = BIOMETRIC_FAIL_KEY + employeeId + ":*";
            String blockPattern = BIOMETRIC_BLOCK_KEY + employeeId + ":*";
            
            Set<String> failKeys = redisTemplate.keys(failPattern);
            Set<String> blockKeys = redisTemplate.keys(blockPattern);
            
            int deletedCount = 0;
            if (failKeys != null && !failKeys.isEmpty()) {
                redisTemplate.delete(failKeys);
                deletedCount += failKeys.size();
            }
            
            if (blockKeys != null && !blockKeys.isEmpty()) {
                redisTemplate.delete(blockKeys);
                deletedCount += blockKeys.size();
            }
            
            log.info("Fallback: 사용자의 모든 디바이스 차단 해제 - 사원번호: {}, 삭제된 키: {}", 
                employeeId, deletedCount);
        } catch (Exception e) {
            log.error("Fallback 모든 디바이스 차단 해제 중 오류 발생 - 사원번호: {}", employeeId, e);
        }
    }

    /**
     * 입력값 검증
     */
    private void validateInput(String employeeId, String deviceId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException("사원번호는 필수입니다");
        }
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("디바이스 ID는 필수입니다");
        }
    }
}
