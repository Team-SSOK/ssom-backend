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

class BiometricRegistrationRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("BiometricRegistrationRequestDto 유효한 데이터 생성 테스트")
    void createValidBiometricRegistrationRequestDto() {
        // given
        String biometricType = "FINGERPRINT";
        String deviceId = "device12345";
        String biometricHash = "hash123456789";
        String deviceInfo = "iPhone 15 Pro, iOS 17.0";

        // when
        BiometricRegistrationRequestDto dto = BiometricRegistrationRequestDto.builder()
                .biometricType(biometricType)
                .deviceId(deviceId)
                .biometricHash(biometricHash)
                .deviceInfo(deviceInfo)
                .build();

        // then
        assertThat(dto.getBiometricType()).isEqualTo(biometricType);
        assertThat(dto.getDeviceId()).isEqualTo(deviceId);
        assertThat(dto.getBiometricHash()).isEqualTo(biometricHash);
        assertThat(dto.getDeviceInfo()).isEqualTo(deviceInfo);

        Set<ConstraintViolation<BiometricRegistrationRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("BiometricRegistrationRequestDto biometricType NotBlank 검증 테스트")
    void validateBiometricTypeNotBlank() {
        // given
        BiometricRegistrationRequestDto dto = BiometricRegistrationRequestDto.builder()
                .biometricType("")
                .deviceId("device12345")
                .biometricHash("hash123456789")
                .build();

        // when
        Set<ConstraintViolation<BiometricRegistrationRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(2); // NotBlank + Pattern validation 모두 발생
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "생체인증 타입은 필수입니다.",
                        "유효하지 않은 생체인증 타입입니다."
                );
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .allMatch(path -> path.equals("biometricType"));
    }

    @Test
    @DisplayName("BiometricRegistrationRequestDto biometricType Pattern 검증 테스트")
    void validateBiometricTypePattern() {
        // given - 유효한 타입들
        BiometricRegistrationRequestDto fingerprintDto = BiometricRegistrationRequestDto.builder()
                .biometricType("FINGERPRINT")
                .deviceId("device12345")
                .biometricHash("hash123456789")
                .build();

        BiometricRegistrationRequestDto faceDto = BiometricRegistrationRequestDto.builder()
                .biometricType("FACE")
                .deviceId("device12345")
                .biometricHash("hash123456789")
                .build();

        // given - 유효하지 않은 타입
        BiometricRegistrationRequestDto invalidDto = BiometricRegistrationRequestDto.builder()
                .biometricType("IRIS")
                .deviceId("device12345")
                .biometricHash("hash123456789")
                .build();

        // when
        Set<ConstraintViolation<BiometricRegistrationRequestDto>> fingerprintViolations = validator.validate(fingerprintDto);
        Set<ConstraintViolation<BiometricRegistrationRequestDto>> faceViolations = validator.validate(faceDto);
        Set<ConstraintViolation<BiometricRegistrationRequestDto>> invalidViolations = validator.validate(invalidDto);

        // then
        assertThat(fingerprintViolations).isEmpty();
        assertThat(faceViolations).isEmpty();
        assertThat(invalidViolations).hasSize(1);
        assertThat(invalidViolations.iterator().next().getMessage()).isEqualTo("유효하지 않은 생체인증 타입입니다.");
    }

    @Test
    @DisplayName("BiometricRegistrationRequestDto deviceId NotBlank 검증 테스트")
    void validateDeviceIdNotBlank() {
        // given
        BiometricRegistrationRequestDto dto = BiometricRegistrationRequestDto.builder()
                .biometricType("FINGERPRINT")
                .deviceId("")
                .biometricHash("hash123456789")
                .build();

        // when
        Set<ConstraintViolation<BiometricRegistrationRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(2); // NotBlank + Size validation 모두 발생
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "디바이스 ID는 필수입니다.",
                        "디바이스 ID는 10-255자 사이여야 합니다."
                );
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .allMatch(path -> path.equals("deviceId"));
    }

    @Test
    @DisplayName("BiometricRegistrationRequestDto deviceId Size 검증 테스트")
    void validateDeviceIdSize() {
        // given - 너무 짧은 deviceId
        BiometricRegistrationRequestDto shortDto = BiometricRegistrationRequestDto.builder()
                .biometricType("FINGERPRINT")
                .deviceId("short")
                .biometricHash("hash123456789")
                .build();

        // given - 너무 긴 deviceId
        StringBuilder longDeviceId = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            longDeviceId.append("a");
        }
        BiometricRegistrationRequestDto longDto = BiometricRegistrationRequestDto.builder()
                .biometricType("FINGERPRINT")
                .deviceId(longDeviceId.toString())
                .biometricHash("hash123456789")
                .build();

        // when
        Set<ConstraintViolation<BiometricRegistrationRequestDto>> shortViolations = validator.validate(shortDto);
        Set<ConstraintViolation<BiometricRegistrationRequestDto>> longViolations = validator.validate(longDto);

        // then
        assertThat(shortViolations).hasSize(1);
        assertThat(longViolations).hasSize(1);
        assertThat(shortViolations.iterator().next().getMessage()).isEqualTo("디바이스 ID는 10-255자 사이여야 합니다.");
        assertThat(longViolations.iterator().next().getMessage()).isEqualTo("디바이스 ID는 10-255자 사이여야 합니다.");
    }

    @Test
    @DisplayName("BiometricRegistrationRequestDto biometricHash NotBlank 검증 테스트")
    void validateBiometricHashNotBlank() {
        // given
        BiometricRegistrationRequestDto dto = BiometricRegistrationRequestDto.builder()
                .biometricType("FINGERPRINT")
                .deviceId("device12345")
                .biometricHash("")
                .build();

        // when
        Set<ConstraintViolation<BiometricRegistrationRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<BiometricRegistrationRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("생체인증 해시는 필수입니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("biometricHash");
    }

    @Test
    @DisplayName("BiometricRegistrationRequestDto biometricHash Size 검증 테스트")
    void validateBiometricHashSize() {
        // given - 너무 긴 biometricHash
        StringBuilder longHash = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            longHash.append("a");
        }
        BiometricRegistrationRequestDto dto = BiometricRegistrationRequestDto.builder()
                .biometricType("FINGERPRINT")
                .deviceId("device12345")
                .biometricHash(longHash.toString())
                .build();

        // when
        Set<ConstraintViolation<BiometricRegistrationRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<BiometricRegistrationRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("생체인증 해시는 500자를 초과할 수 없습니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("biometricHash");
    }

    @Test
    @DisplayName("BiometricRegistrationRequestDto 모든 필수 필드 null 검증 테스트")
    void validateAllRequiredFieldsNull() {
        // given
        BiometricRegistrationRequestDto dto = BiometricRegistrationRequestDto.builder()
                .biometricType(null)
                .deviceId(null)
                .biometricHash(null)
                .deviceInfo(null)
                .build();

        // when
        Set<ConstraintViolation<BiometricRegistrationRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(3); // biometricType, deviceId, biometricHash
    }
}
