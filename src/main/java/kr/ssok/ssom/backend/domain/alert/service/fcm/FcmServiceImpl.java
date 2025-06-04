package kr.ssok.ssom.backend.domain.alert.service.fcm;

import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * FCM 토큰 관리 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${fcm.token-ttl-seconds}")
    private long ttlSeconds;

    /**
     * FCM 토큰을 Redis에 등록
     *
     * @param userId 사용자 ID
     * @param token  FCM 토큰
     */
    @Override
    public void registerFcmToken(Long userId, String token) {
        validateToken(token);

        try {
            String key = userKey(userId);
            String existingToken = redisTemplate.opsForValue().get(key);

            if (existingToken != null) {
                if (existingToken.equals(token)) {
                    log.debug("동일한 FCM 토큰이 이미 등록되어 있습니다. 갱신하지 않습니다. userId: {}", userId);
                    return;
                }
                log.info("기존 토큰({})이 존재하여 삭제 후 재등록합니다. userId: {}", existingToken, userId);
                redisTemplate.delete(key);
            }

            // 새로운 토큰 등록 또는 갱신
            redisTemplate.opsForValue().set(key, token, Duration.ofSeconds(ttlSeconds));
            log.info("FCM 토큰 등록 완료. userId: {}, token: {}", userId, token);

        } catch (DataAccessException e) {
            log.error("Redis 접근 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("Redis 접근 중 오류 발생");
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생: {}", e.getMessage());
            throw new RuntimeException("예상치 못한 오류 발생d");
        }
    }

    /**
     * 토큰 유효성 검사
     *
     * @param token FCM 토큰
     */
    private void validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("FCM 토큰이 유효하지 않습니다.");
            throw new RuntimeException("FCM 토큰이 유효하지 않습니다.");
        }
    }

    /**
     * 사용자 Redis 키 생성
     *
     * @param userId 사용자 ID
     * @return Redis 키
     */
    private String userKey(Long userId) {
        return "userfcm:" + userId;
    }
}
