package kr.ssok.ssom.backend.domain.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRefreshRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("TokenRefreshRequestDto 유효한 데이터 생성 테스트")
    void createValidTokenRefreshRequestDto() {
        // given
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        // when
        TokenRefreshRequestDto dto = new TokenRefreshRequestDto(refreshToken);

        // then
        assertThat(dto.getRefreshToken()).isEqualTo(refreshToken);

        Set<ConstraintViolation<TokenRefreshRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TokenRefreshRequestDto refreshToken NotBlank 검증 테스트")
    void validateRefreshTokenNotBlank() {
        // given
        TokenRefreshRequestDto dto = new TokenRefreshRequestDto("");

        // when
        Set<ConstraintViolation<TokenRefreshRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<TokenRefreshRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Refresh Token이 비어있습니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("TokenRefreshRequestDto refreshToken null 검증 테스트")
    void validateRefreshTokenNull() {
        // given
        TokenRefreshRequestDto dto = new TokenRefreshRequestDto(null);

        // when
        Set<ConstraintViolation<TokenRefreshRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<TokenRefreshRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Refresh Token이 비어있습니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("TokenRefreshRequestDto 기본 생성자 테스트")
    void createTokenRefreshRequestDtoWithNoArgsConstructor() {
        // when
        TokenRefreshRequestDto dto = new TokenRefreshRequestDto();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("TokenRefreshRequestDto Setter 테스트")
    void testTokenRefreshRequestDtoSetter() {
        // given
        TokenRefreshRequestDto dto = new TokenRefreshRequestDto();
        String refreshToken = "new.refresh.token.value";

        // when
        dto.setRefreshToken(refreshToken);

        // then
        assertThat(dto.getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("TokenRefreshRequestDto 공백 문자열 검증 테스트")
    void validateRefreshTokenWhitespace() {
        // given
        TokenRefreshRequestDto dto = new TokenRefreshRequestDto("   ");

        // when
        Set<ConstraintViolation<TokenRefreshRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<TokenRefreshRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Refresh Token이 비어있습니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("TokenRefreshRequestDto 다양한 토큰 형식 테스트")
    void testVariousTokenFormats() {
        // given
        String[] validTokens = {
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                "simple.token.value",
                "token123",
                "Bearer token",
                "a1b2c3d4e5f6"
        };

        for (String token : validTokens) {
            // when
            TokenRefreshRequestDto dto = new TokenRefreshRequestDto(token);
            Set<ConstraintViolation<TokenRefreshRequestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
            assertThat(dto.getRefreshToken()).isEqualTo(token);
        }
    }

    @Test
    @DisplayName("TokenRefreshRequestDto 긴 토큰 문자열 테스트")
    void testLongTokenString() {
        // given
        StringBuilder longToken = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longToken.append("a");
        }
        String refreshToken = longToken.toString();

        // when
        TokenRefreshRequestDto dto = new TokenRefreshRequestDto(refreshToken);
        Set<ConstraintViolation<TokenRefreshRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
        assertThat(dto.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(dto.getRefreshToken().length()).isEqualTo(1000);
    }
}
