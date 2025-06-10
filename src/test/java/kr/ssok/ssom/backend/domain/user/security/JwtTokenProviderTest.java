package kr.ssok.ssom.backend.domain.user.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kr.ssok.ssom.backend.domain.user.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET_KEY = "testSecretKeyForJwtTokenProviderTestSecretKey123456789";
    private static final long ACCESS_TOKEN_VALIDITY = 3600; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 86400; // 24시간
    private static final String TEST_USER_ID = "CHN0001";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidityInSeconds", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidityInSeconds", REFRESH_TOKEN_VALIDITY);

        jwtTokenProvider.init();
    }

    @Nested
    @DisplayName("토큰 생성 테스트")
    class TokenCreationTest {

        @Test
        @DisplayName("Access Token이 정상적으로 생성된다")
        void createAccessToken_Success() {
            // when
            String accessToken = jwtTokenProvider.createAccessToken(TEST_USER_ID);

            // then
            assertThat(accessToken).isNotNull();
            assertThat(accessToken).isNotEmpty();
            assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
            assertThat(jwtTokenProvider.getUserIdFromToken(accessToken)).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Refresh Token이 정상적으로 생성된다")
        void createRefreshToken_Success() {
            // when
            String refreshToken = jwtTokenProvider.createRefreshToken(TEST_USER_ID);

            // then
            assertThat(refreshToken).isNotNull();
            assertThat(refreshToken).isNotEmpty();
            assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
            assertThat(jwtTokenProvider.getUserIdFromToken(refreshToken)).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Access Token과 Refresh Token이 서로 다르다")
        void accessTokenAndRefreshTokenAreDifferent() {
            // when
            String accessToken = jwtTokenProvider.createAccessToken(TEST_USER_ID);
            String refreshToken = jwtTokenProvider.createRefreshToken(TEST_USER_ID);

            // then
            assertThat(accessToken).isNotEqualTo(refreshToken);
        }

        @Test
        @DisplayName("null userId로 토큰 생성 시 예외가 발생하지 않는다")
        void createToken_WithNullUserId_DoesNotThrowException() {
            // when & then
            assertThatCode(() -> jwtTokenProvider.createAccessToken(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("빈 문자열 userId로 토큰 생성이 가능하다")
        void createToken_WithEmptyUserId_Success() {
            // when
            String token = jwtTokenProvider.createAccessToken("");

            // then
            assertThat(token).isNotNull();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("토큰 검증 테스트")
    class TokenValidationTest {

        @Test
        @DisplayName("유효한 토큰 검증이 성공한다")
        void validateToken_ValidToken_ReturnsTrue() {
            // given
            String token = jwtTokenProvider.createAccessToken(TEST_USER_ID);

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("null 토큰 검증 시 false를 반환한다")
        void validateToken_NullToken_ReturnsFalse() {
            // when
            boolean isValid = jwtTokenProvider.validateToken(null);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 토큰 검증 시 false를 반환한다")
        void validateToken_EmptyToken_ReturnsFalse() {
            // when
            boolean isValid = jwtTokenProvider.validateToken("");

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰 검증 시 false를 반환한다")
        void validateToken_InvalidFormatToken_ReturnsFalse() {
            // when
            boolean isValid = jwtTokenProvider.validateToken("invalid.token.format");

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰 검증 시 false를 반환한다")
        void validateToken_ExpiredToken_ReturnsFalse() throws InterruptedException {
            // given - 매우 짧은 유효시간으로 토큰 생성
            ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidityInSeconds", 1L);
            jwtTokenProvider.init();

            String token = jwtTokenProvider.createAccessToken(TEST_USER_ID);

            // 토큰이 만료될 때까지 대기
            Thread.sleep(1100);

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("잘못된 서명으로 생성된 토큰 검증 시 false를 반환한다")
        void validateToken_InvalidSignatureToken_ReturnsFalse() {
            // given - 다른 키로 생성된 토큰
            String wrongKey = "wrongSecretKeyForTestingPurposesOnly123456789";
            String encodedWrongKey = Base64.getEncoder().encodeToString(wrongKey.getBytes());
            Key wrongSigningKey = Keys.hmacShaKeyFor(encodedWrongKey.getBytes());

            String tokenWithWrongSignature = Jwts.builder()
                    .claim("userId", TEST_USER_ID)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(wrongSigningKey)
                    .compact();

            // when
            boolean isValid = jwtTokenProvider.validateToken(tokenWithWrongSignature);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("사용자 ID 추출 테스트")
    class UserIdExtractionTest {

        @Test
        @DisplayName("유효한 토큰에서 사용자 ID를 정상적으로 추출한다")
        void getUserIdFromToken_ValidToken_ReturnsUserId() {
            // given
            String token = jwtTokenProvider.createAccessToken(TEST_USER_ID);

            // when
            String extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("null 토큰에서 사용자 ID 추출 시 null을 반환한다")
        void getUserIdFromToken_NullToken_ReturnsNull() {
            // when
            String userId = jwtTokenProvider.getUserIdFromToken(null);

            // then
            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰에서 사용자 ID 추출 시 null을 반환한다")
        void getUserIdFromToken_InvalidToken_ReturnsNull() {
            // when
            String userId = jwtTokenProvider.getUserIdFromToken("invalid.token");

            // then
            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("만료된 토큰에서 사용자 ID 추출 시 null을 반환한다")
        void getUserIdFromToken_ExpiredToken_ReturnsNull() throws InterruptedException {
            // given
            ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidityInSeconds", 1L);
            jwtTokenProvider.init();

            String token = jwtTokenProvider.createAccessToken(TEST_USER_ID);
            Thread.sleep(1100);

            // when
            String userId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("여러 다른 사용자 ID로 생성된 토큰에서 올바른 ID를 추출한다")
        void getUserIdFromToken_DifferentUserIds_ReturnsCorrectIds() {
            // given
            String userId1 = "CHN0001";
            String userId2 = "CORE0002";
            String userId3 = "EXT0003";

            String token1 = jwtTokenProvider.createAccessToken(userId1);
            String token2 = jwtTokenProvider.createAccessToken(userId2);
            String token3 = jwtTokenProvider.createAccessToken(userId3);

            // when & then
            assertThat(jwtTokenProvider.getUserIdFromToken(token1)).isEqualTo(userId1);
            assertThat(jwtTokenProvider.getUserIdFromToken(token2)).isEqualTo(userId2);
            assertThat(jwtTokenProvider.getUserIdFromToken(token3)).isEqualTo(userId3);
        }
    }

    @Nested
    @DisplayName("토큰 만료시간 계산 테스트")
    class TokenExpirationTest {

        @Test
        @DisplayName("유효한 토큰의 만료시간을 정확히 계산한다")
        void getTokenExpirationTime_ValidToken_ReturnsCorrectTime() {
            // given
            String token = jwtTokenProvider.createAccessToken(TEST_USER_ID);

            // when
            long expirationTime = jwtTokenProvider.getTokenExpirationTime(token);

            // then
            assertThat(expirationTime).isGreaterThan(0);
            assertThat(expirationTime).isLessThanOrEqualTo(ACCESS_TOKEN_VALIDITY);
        }

        @Test
        @DisplayName("null 토큰의 만료시간 계산 시 0을 반환한다")
        void getTokenExpirationTime_NullToken_ReturnsZero() {
            // when
            long expirationTime = jwtTokenProvider.getTokenExpirationTime(null);

            // then
            assertThat(expirationTime).isEqualTo(0);
        }

        @Test
        @DisplayName("잘못된 토큰의 만료시간 계산 시 0을 반환한다")
        void getTokenExpirationTime_InvalidToken_ReturnsZero() {
            // when
            long expirationTime = jwtTokenProvider.getTokenExpirationTime("invalid.token");

            // then
            assertThat(expirationTime).isEqualTo(0);
        }

        @Test
        @DisplayName("Access Token과 Refresh Token의 만료시간이 다르다")
        void getTokenExpirationTime_DifferentTokenTypes_DifferentExpirationTimes() {
            // given
            String accessToken = jwtTokenProvider.createAccessToken(TEST_USER_ID);
            String refreshToken = jwtTokenProvider.createRefreshToken(TEST_USER_ID);

            // when
            long accessExpirationTime = jwtTokenProvider.getTokenExpirationTime(accessToken);
            long refreshExpirationTime = jwtTokenProvider.getTokenExpirationTime(refreshToken);

            // then
            assertThat(refreshExpirationTime).isGreaterThan(accessExpirationTime);
        }
    }

    @Nested
    @DisplayName("Bearer 토큰 해결 테스트")
    class BearerTokenResolveTest {

        @Test
        @DisplayName("Bearer 토큰에서 JWT를 정상적으로 추출한다")
        void resolveToken_ValidBearerToken_ReturnsJwt() {
            // given
            String jwt = jwtTokenProvider.createAccessToken(TEST_USER_ID);
            String bearerToken = "Bearer " + jwt;

            // when
            String resolvedToken = jwtTokenProvider.resolveToken(bearerToken);

            // then
            assertThat(resolvedToken).isEqualTo(jwt);
        }

        @Test
        @DisplayName("Bearer 프리픽스가 없는 토큰에서 null을 반환한다")
        void resolveToken_WithoutBearerPrefix_ReturnsNull() {
            // given
            String jwt = jwtTokenProvider.createAccessToken(TEST_USER_ID);

            // when
            String resolvedToken = jwtTokenProvider.resolveToken(jwt);

            // then
            assertThat(resolvedToken).isNull();
        }

        @Test
        @DisplayName("null 토큰 해결 시 null을 반환한다")
        void resolveToken_NullToken_ReturnsNull() {
            // when
            String resolvedToken = jwtTokenProvider.resolveToken(null);

            // then
            assertThat(resolvedToken).isNull();
        }

        @Test
        @DisplayName("빈 문자열 토큰 해결 시 null을 반환한다")
        void resolveToken_EmptyToken_ReturnsNull() {
            // when
            String resolvedToken = jwtTokenProvider.resolveToken("");

            // then
            assertThat(resolvedToken).isNull();
        }

        @Test
        @DisplayName("Bearer만 있고 토큰이 없는 경우 빈 문자열을 반환한다")
        void resolveToken_BearerOnly_ReturnsEmptyString() {
            // when
            String resolvedToken = jwtTokenProvider.resolveToken("Bearer ");

            // then
            assertThat(resolvedToken).isEmpty();
        }

        @Test
        @DisplayName("대소문자가 다른 Bearer 프리픽스는 처리하지 않는다")
        void resolveToken_CaseInsensitiveBearer_ReturnsNull() {
            // given
            String jwt = jwtTokenProvider.createAccessToken(TEST_USER_ID);

            // when
            String resolvedToken1 = jwtTokenProvider.resolveToken("bearer " + jwt);
            String resolvedToken2 = jwtTokenProvider.resolveToken("BEARER " + jwt);

            // then
            assertThat(resolvedToken1).isNull();
            assertThat(resolvedToken2).isNull();
        }
    }

    @Nested
    @DisplayName("초기화 테스트")
    class InitializationTest {

        @Test
        @DisplayName("init 메서드 호출 시 정상적으로 초기화된다")
        void init_Success() {
            // given
            JwtTokenProvider newProvider = new JwtTokenProvider();
            ReflectionTestUtils.setField(newProvider, "secretKey", TEST_SECRET_KEY);
            ReflectionTestUtils.setField(newProvider, "accessTokenValidityInSeconds", ACCESS_TOKEN_VALIDITY);
            ReflectionTestUtils.setField(newProvider, "refreshTokenValidityInSeconds", REFRESH_TOKEN_VALIDITY);

            // when & then
            assertThatCode(() -> newProvider.init()).doesNotThrowAnyException();

            // 초기화 후 토큰 생성이 가능한지 확인
            String token = newProvider.createAccessToken(TEST_USER_ID);
            assertThat(token).isNotNull();
            assertThat(newProvider.validateToken(token)).isTrue();
        }
    }
}