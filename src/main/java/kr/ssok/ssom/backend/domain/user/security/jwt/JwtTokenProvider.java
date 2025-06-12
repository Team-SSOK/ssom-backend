package kr.ssok.ssom.backend.domain.user.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity-in-seconds}")
    private long accessTokenValidityInSeconds;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidityInSeconds;

    @Value("${jwt.sse-token-validity-in-seconds}")
    private long sseTokenValidityInSeconds;

    private Key key;

    @PostConstruct
    public void init() {
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
        key = Keys.hmacShaKeyFor(encodedKey.getBytes());
        log.info("JWT key initialized");
    }

    /**
     * Access Token을 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 생성된 JWT Access Token
     */
    public String createAccessToken(String userId) {
        return createToken(userId, accessTokenValidityInSeconds * 1000);
    }

    /**
     * Refresh Token을 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 생성된 JWT Refresh Token
     */
    public String createRefreshToken(String userId) {
        return createToken(userId, refreshTokenValidityInSeconds * 1000);
    }

    /**
     * SSE 전용 장기 토큰을 생성합니다.
     * 
     * @param userId 사용자 ID
     * @return 생성된 JWT SSE Token
     */
    public String createSseToken(String userId) {
        return createSseTokenInternal(userId, sseTokenValidityInSeconds * 1000);
    }

    /**
     * 토큰 생성 메소드
     */
    private String createToken(String userId, long validityInMilliseconds) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * SSE 토큰 생성 메소드 (type 클레임 추가)
     */
    private String createSseTokenInternal(String userId, long validityInMilliseconds) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "SSE"); // SSE 토큰임을 명시

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 userId(사원번호) 추출
     *
     * @param token JWT 토큰
     * @return 토큰에 저장된 사용자 ID (사원번호), 실패 시 null
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("userId").toString();
        } catch (Exception e) {
            log.error("Could not get userId from token: {}", e.getMessage());
            return null; // 예외 발생 대신 null 반환 (Filter에서 처리)
        }
    }

    /**
     * 토큰 유효성 검증
     *
     * @param token 검증할 JWT 토큰
     * @return 토큰 유효성 여부 (true: 유효, false: 유효하지 않음)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * SSE 토큰 여부 확인
     * 
     * @param token JWT 토큰
     * @return SSE 토큰이면 true, 일반 토큰이면 false
     */
    public boolean isSseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenType = (String) claims.get("tokenType");
            return "SSE".equals(tokenType);
        } catch (Exception e) {
            log.debug("Could not check token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 남은 유효시간 계산
     *
     * @param token JWT 토큰
     * @return 토큰 남은 유효시간 (초 단위)
     */
    public long getTokenExpirationTime(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            Date now = new Date();

            return (expiration.getTime() - now.getTime()) / 1000; // 초 단위 변환
        } catch (Exception e) {
            log.error("Could not calculate token expiration time: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Bearer 토큰에서 JWT 추출
     *
     * @param bearerToken Bearer 토큰
     * @return JWT 토큰 (Bearer 프리픽스 제거된)
     */
    public String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
