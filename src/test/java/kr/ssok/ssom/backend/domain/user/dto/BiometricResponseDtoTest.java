package kr.ssok.ssom.backend.domain.user.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class BiometricResponseDtoTest {

    @Test
    @DisplayName("BiometricResponseDto 성공 응답 생성 테스트")
    void createSuccessfulBiometricResponseDto() {
        // given
        Boolean success = true;
        String message = "생체인증이 성공적으로 완료되었습니다.";
        Long biometricId = 12345L;

        // when
        BiometricResponseDto dto = BiometricResponseDto.builder()
                .success(success)
                .message(message)
                .biometricId(biometricId)
                .build();

        // then
        assertThat(dto.getSuccess()).isTrue();
        assertThat(dto.getMessage()).isEqualTo(message);
        assertThat(dto.getBiometricId()).isEqualTo(biometricId);
        assertThat(dto.getErrorCode()).isNull();
        assertThat(dto.getRemainingAttempts()).isNull();
    }

    @Test
    @DisplayName("BiometricResponseDto 실패 응답 생성 테스트")
    void createFailedBiometricResponseDto() {
        // given
        Boolean success = false;
        String message = "생체인증에 실패했습니다.";
        String errorCode = "BIOMETRIC_VERIFICATION_FAILED";
        Integer remainingAttempts = 2;

        // when
        BiometricResponseDto dto = BiometricResponseDto.builder()
                .success(success)
                .message(message)
                .errorCode(errorCode)
                .remainingAttempts(remainingAttempts)
                .build();

        // then
        assertThat(dto.getSuccess()).isFalse();
        assertThat(dto.getMessage()).isEqualTo(message);
        assertThat(dto.getBiometricId()).isNull();
        assertThat(dto.getErrorCode()).isEqualTo(errorCode);
        assertThat(dto.getRemainingAttempts()).isEqualTo(remainingAttempts);
    }

    @Test
    @DisplayName("BiometricResponseDto 기본 생성자 테스트")
    void createBiometricResponseDtoWithNoArgsConstructor() {
        // when
        BiometricResponseDto dto = new BiometricResponseDto();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getSuccess()).isNull();
        assertThat(dto.getMessage()).isNull();
        assertThat(dto.getBiometricId()).isNull();
        assertThat(dto.getErrorCode()).isNull();
        assertThat(dto.getRemainingAttempts()).isNull();
    }

    @Test
    @DisplayName("BiometricResponseDto 전체 생성자 테스트")
    void createBiometricResponseDtoWithAllArgsConstructor() {
        // given
        Boolean success = true;
        String message = "생체인증 등록 완료";
        Long biometricId = 67890L;
        String errorCode = null;
        Integer remainingAttempts = null;

        // when
        BiometricResponseDto dto = new BiometricResponseDto(
                success, message, biometricId, errorCode, remainingAttempts
        );

        // then
        assertThat(dto.getSuccess()).isTrue();
        assertThat(dto.getMessage()).isEqualTo(message);
        assertThat(dto.getBiometricId()).isEqualTo(biometricId);
        assertThat(dto.getErrorCode()).isNull();
        assertThat(dto.getRemainingAttempts()).isNull();
    }

    @Test
    @DisplayName("BiometricResponseDto Setter 테스트")
    void testBiometricResponseDtoSetter() {
        // given
        BiometricResponseDto dto = new BiometricResponseDto();
        Boolean success = false;
        String message = "시도 횟수 초과";
        Long biometricId = null;
        String errorCode = "MAX_ATTEMPTS_EXCEEDED";
        Integer remainingAttempts = 0;

        // when
        dto.setSuccess(success);
        dto.setMessage(message);
        dto.setBiometricId(biometricId);
        dto.setErrorCode(errorCode);
        dto.setRemainingAttempts(remainingAttempts);

        // then
        assertThat(dto.getSuccess()).isFalse();
        assertThat(dto.getMessage()).isEqualTo(message);
        assertThat(dto.getBiometricId()).isNull();
        assertThat(dto.getErrorCode()).isEqualTo(errorCode);
        assertThat(dto.getRemainingAttempts()).isEqualTo(remainingAttempts);
    }

    @Test
    @DisplayName("BiometricResponseDto 등록 성공 시나리오 테스트")
    void testRegistrationSuccessScenario() {
        // when
        BiometricResponseDto dto = BiometricResponseDto.builder()
                .success(true)
                .message("지문 등록이 완료되었습니다.")
                .biometricId(100L)
                .build();

        // then
        assertThat(dto.getSuccess()).isTrue();
        assertThat(dto.getMessage()).contains("등록이 완료");
        assertThat(dto.getBiometricId()).isNotNull();
        assertThat(dto.getErrorCode()).isNull();
        assertThat(dto.getRemainingAttempts()).isNull();
    }

    @Test
    @DisplayName("BiometricResponseDto 인증 실패 시나리오 테스트")
    void testAuthenticationFailureScenario() {
        // when
        BiometricResponseDto dto = BiometricResponseDto.builder()
                .success(false)
                .message("지문 인식에 실패했습니다.")
                .errorCode("FINGERPRINT_NOT_RECOGNIZED")
                .remainingAttempts(1)
                .build();

        // then
        assertThat(dto.getSuccess()).isFalse();
        assertThat(dto.getMessage()).contains("실패");
        assertThat(dto.getBiometricId()).isNull();
        assertThat(dto.getErrorCode()).isEqualTo("FINGERPRINT_NOT_RECOGNIZED");
        assertThat(dto.getRemainingAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("BiometricResponseDto 계정 잠금 시나리오 테스트")
    void testAccountLockScenario() {
        // when
        BiometricResponseDto dto = BiometricResponseDto.builder()
                .success(false)
                .message("생체인증 시도 횟수를 초과하여 계정이 잠겼습니다.")
                .errorCode("ACCOUNT_LOCKED")
                .remainingAttempts(0)
                .build();

        // then
        assertThat(dto.getSuccess()).isFalse();
        assertThat(dto.getMessage()).contains("잠겼습니다");
        assertThat(dto.getBiometricId()).isNull();
        assertThat(dto.getErrorCode()).isEqualTo("ACCOUNT_LOCKED");
        assertThat(dto.getRemainingAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("BiometricResponseDto 다양한 에러 코드 테스트")
    void testVariousErrorCodes() {
        // given
        String[] errorCodes = {
                "BIOMETRIC_VERIFICATION_FAILED",
                "DEVICE_NOT_REGISTERED",
                "BIOMETRIC_DATA_CORRUPTED",
                "SECURITY_VIOLATION",
                "MAX_ATTEMPTS_EXCEEDED"
        };

        for (String errorCode : errorCodes) {
            // when
            BiometricResponseDto dto = BiometricResponseDto.builder()
                    .success(false)
                    .message("에러 발생: " + errorCode)
                    .errorCode(errorCode)
                    .remainingAttempts(3)
                    .build();

            // then
            assertThat(dto.getSuccess()).isFalse();
            assertThat(dto.getErrorCode()).isEqualTo(errorCode);
            assertThat(dto.getRemainingAttempts()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("BiometricResponseDto 잔여 시도 횟수 테스트")
    void testRemainingAttemptsValues() {
        // given & when & then
        for (int i = 0; i <= 5; i++) {
            BiometricResponseDto dto = BiometricResponseDto.builder()
                    .success(false)
                    .remainingAttempts(i)
                    .build();

            assertThat(dto.getRemainingAttempts()).isEqualTo(i);
        }
    }
}
