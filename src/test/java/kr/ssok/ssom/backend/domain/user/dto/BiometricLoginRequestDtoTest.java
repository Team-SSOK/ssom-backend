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

class BiometricLoginRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("BiometricLoginRequestDto 유효한 데이터 생성 테스트")
    void createValidBiometricLoginRequestDto() {
        // given
        String employeeId = "CHN0001";
        String biometricType = "FINGERPRINT";
        String deviceId = "device123";
        String biometricHash = "hash123456";
        Long timestamp = System.currentTimeMillis();

        // when
        BiometricLoginRequestDto dto = BiometricLoginRequestDto.builder()
                .employeeId(employeeId)
                .biometricType(biometricType)
                .deviceId(deviceId)
                .biometricHash(biometricHash)
                .timestamp(timestamp)
                .build();

        // then
        assertThat(dto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(dto.getBiometricType()).isEqualTo(biometricType);
        assertThat(dto.getDeviceId()).isEqualTo(deviceId);
        assertThat(dto.getBiometricHash()).isEqualTo(biometricHash);
        assertThat(dto.getTimestamp()).isEqualTo(timestamp);

        Set<ConstraintViolation<BiometricLoginRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("BiometricLoginRequestDto employeeId NotBlank 검증 테스트")
    void validateEmployeeIdNotBlank() {
        // given
        BiometricLoginRequestDto dto = BiometricLoginRequestDto.builder()
                .employeeId("")
                .biometricType("FINGERPRINT")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(System.currentTimeMillis())
                .build();

        // when
        Set<ConstraintViolation<BiometricLoginRequestDto>> violations = validator.validate(dto);

        // then - 빈 문자열은 @NotBlank와 @Size 두 조건 모두 위반
        assertThat(violations).hasSize(2);
        
        // 두 에러 메시지가 모두 있는지 확인
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
        
        assertThat(messages).contains("사원번호는 필수입니다.");
        assertThat(messages).contains("사원번호는 4-10자 사이여야 합니다.");
        
        violations.forEach(violation -> 
            assertThat(violation.getPropertyPath().toString()).isEqualTo("employeeId")
        );
    }

    @Test
    @DisplayName("BiometricLoginRequestDto employeeId Size 검증 테스트")
    void validateEmployeeIdSize() {
        // given - 너무 짧은 사원번호
        BiometricLoginRequestDto shortDto = BiometricLoginRequestDto.builder()
                .employeeId("123")
                .biometricType("FINGERPRINT")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(System.currentTimeMillis())
                .build();

        // given - 너무 긴 사원번호
        BiometricLoginRequestDto longDto = BiometricLoginRequestDto.builder()
                .employeeId("12345678901")
                .biometricType("FINGERPRINT")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(System.currentTimeMillis())
                .build();

        // when
        Set<ConstraintViolation<BiometricLoginRequestDto>> shortViolations = validator.validate(shortDto);
        Set<ConstraintViolation<BiometricLoginRequestDto>> longViolations = validator.validate(longDto);

        // then
        assertThat(shortViolations).hasSize(1);
        assertThat(longViolations).hasSize(1);
        assertThat(shortViolations.iterator().next().getMessage()).isEqualTo("사원번호는 4-10자 사이여야 합니다.");
        assertThat(longViolations.iterator().next().getMessage()).isEqualTo("사원번호는 4-10자 사이여야 합니다.");
    }

    @Test
    @DisplayName("BiometricLoginRequestDto biometricType Pattern 검증 테스트")
    void validateBiometricTypePattern() {
        // given - 유효한 타입들
        BiometricLoginRequestDto fingerprintDto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FINGERPRINT")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(System.currentTimeMillis())
                .build();

        BiometricLoginRequestDto faceDto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FACE")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(System.currentTimeMillis())
                .build();

        // given - 유효하지 않은 타입
        BiometricLoginRequestDto invalidDto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("INVALID")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(System.currentTimeMillis())
                .build();

        // when
        Set<ConstraintViolation<BiometricLoginRequestDto>> fingerprintViolations = validator.validate(fingerprintDto);
        Set<ConstraintViolation<BiometricLoginRequestDto>> faceViolations = validator.validate(faceDto);
        Set<ConstraintViolation<BiometricLoginRequestDto>> invalidViolations = validator.validate(invalidDto);

        // then
        assertThat(fingerprintViolations).isEmpty();
        assertThat(faceViolations).isEmpty();
        assertThat(invalidViolations).hasSize(1);
        assertThat(invalidViolations.iterator().next().getMessage()).isEqualTo("유효하지 않은 생체인증 타입입니다.");
    }

    @Test
    @DisplayName("BiometricLoginRequestDto deviceId NotBlank 검증 테스트")
    void validateDeviceIdNotBlank() {
        // given
        BiometricLoginRequestDto dto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FINGERPRINT")
                .deviceId("")
                .biometricHash("hash123")
                .timestamp(System.currentTimeMillis())
                .build();

        // when
        Set<ConstraintViolation<BiometricLoginRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<BiometricLoginRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("디바이스 ID는 필수입니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("deviceId");
    }

    @Test
    @DisplayName("BiometricLoginRequestDto biometricHash NotBlank 검증 테스트")
    void validateBiometricHashNotBlank() {
        // given
        BiometricLoginRequestDto dto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FINGERPRINT")
                .deviceId("device123")
                .biometricHash("")
                .timestamp(System.currentTimeMillis())
                .build();

        // when
        Set<ConstraintViolation<BiometricLoginRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<BiometricLoginRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("생체인증 해시는 필수입니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("biometricHash");
    }

    @Test
    @DisplayName("BiometricLoginRequestDto timestamp 검증 테스트")
    void validateTimestamp() {
        // given - null timestamp
        BiometricLoginRequestDto nullDto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FINGERPRINT")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(null)
                .build();

        // given - 유효하지 않은 timestamp (너무 작은 값)
        BiometricLoginRequestDto invalidDto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FINGERPRINT")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(999999999999L)
                .build();

        // when
        Set<ConstraintViolation<BiometricLoginRequestDto>> nullViolations = validator.validate(nullDto);
        Set<ConstraintViolation<BiometricLoginRequestDto>> invalidViolations = validator.validate(invalidDto);

        // then
        assertThat(nullViolations).hasSize(1);
        assertThat(invalidViolations).hasSize(1);
        assertThat(nullViolations.iterator().next().getMessage()).isEqualTo("타임스탬프는 필수입니다.");
        assertThat(invalidViolations.iterator().next().getMessage()).isEqualTo("유효하지 않은 타임스탬프입니다.");
    }

    @Test
    @DisplayName("BiometricLoginRequestDto 기본 생성자 테스트")
    void createBiometricLoginRequestDtoWithNoArgsConstructor() {
        // when
        BiometricLoginRequestDto dto = new BiometricLoginRequestDto();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getEmployeeId()).isNull();
        assertThat(dto.getBiometricType()).isNull();
        assertThat(dto.getDeviceId()).isNull();
        assertThat(dto.getBiometricHash()).isNull();
        assertThat(dto.getTimestamp()).isNull();
        assertThat(dto.getChallengeResponse()).isNull();
        assertThat(dto.getDeviceFingerprint()).isNull();
    }

    @Test
    @DisplayName("BiometricLoginRequestDto 전체 생성자 테스트")
    void createBiometricLoginRequestDtoWithAllArgsConstructor() {
        // given
        String employeeId = "CHN0001";
        String biometricType = "FACE";
        String deviceId = "device456";
        String biometricHash = "hash654321";
        Long timestamp = 1717999800000L;
        String challengeResponse = "challenge123";
        String deviceFingerprint = "fingerprint789";

        // when
        BiometricLoginRequestDto dto = new BiometricLoginRequestDto(
                employeeId, biometricType, deviceId, biometricHash,
                timestamp, challengeResponse, deviceFingerprint
        );

        // then
        assertThat(dto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(dto.getBiometricType()).isEqualTo(biometricType);
        assertThat(dto.getDeviceId()).isEqualTo(deviceId);
        assertThat(dto.getBiometricHash()).isEqualTo(biometricHash);
        assertThat(dto.getTimestamp()).isEqualTo(timestamp);
        assertThat(dto.getChallengeResponse()).isEqualTo(challengeResponse);
        assertThat(dto.getDeviceFingerprint()).isEqualTo(deviceFingerprint);
    }

    @Test
    @DisplayName("BiometricLoginRequestDto Setter 테스트")
    void testBiometricLoginRequestDtoSetter() {
        // given
        BiometricLoginRequestDto dto = new BiometricLoginRequestDto();
        String employeeId = "CHN0001";
        String biometricType = "FINGERPRINT";
        String deviceId = "device789";
        String biometricHash = "hash987654";
        Long timestamp = System.currentTimeMillis();
        String challengeResponse = "challenge456";
        String deviceFingerprint = "fingerprint012";

        // when
        dto.setEmployeeId(employeeId);
        dto.setBiometricType(biometricType);
        dto.setDeviceId(deviceId);
        dto.setBiometricHash(biometricHash);
        dto.setTimestamp(timestamp);
        dto.setChallengeResponse(challengeResponse);
        dto.setDeviceFingerprint(deviceFingerprint);

        // then
        assertThat(dto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(dto.getBiometricType()).isEqualTo(biometricType);
        assertThat(dto.getDeviceId()).isEqualTo(deviceId);
        assertThat(dto.getBiometricHash()).isEqualTo(biometricHash);
        assertThat(dto.getTimestamp()).isEqualTo(timestamp);
        assertThat(dto.getChallengeResponse()).isEqualTo(challengeResponse);
        assertThat(dto.getDeviceFingerprint()).isEqualTo(deviceFingerprint);
    }

    @Test
    @DisplayName("BiometricLoginRequestDto 추가 보안 필드 테스트")
    void testAdditionalSecurityFields() {
        // given
        String challengeResponse = "secure_challenge_response";
        String deviceFingerprint = "unique_device_fingerprint";

        // when
        BiometricLoginRequestDto dto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FINGERPRINT")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(System.currentTimeMillis())
                .challengeResponse(challengeResponse)
                .deviceFingerprint(deviceFingerprint)
                .build();

        // then
        assertThat(dto.getChallengeResponse()).isEqualTo(challengeResponse);
        assertThat(dto.getDeviceFingerprint()).isEqualTo(deviceFingerprint);

        // 추가 보안 필드들은 선택적이므로 검증 오류가 없어야 함
        Set<ConstraintViolation<BiometricLoginRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("BiometricLoginRequestDto 선택적 보안 필드 null 테스트")
    void testOptionalSecurityFieldsNull() {
        // when
        BiometricLoginRequestDto dto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FACE")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(System.currentTimeMillis())
                .challengeResponse(null)
                .deviceFingerprint(null)
                .build();

        // then
        assertThat(dto.getChallengeResponse()).isNull();
        assertThat(dto.getDeviceFingerprint()).isNull();

        // 선택적 필드가 null이어도 검증 오류가 없어야 함
        Set<ConstraintViolation<BiometricLoginRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("BiometricLoginRequestDto 모든 필수 필드 null 검증 테스트")
    void validateAllRequiredFieldsNull() {
        // given
        BiometricLoginRequestDto dto = BiometricLoginRequestDto.builder()
                .employeeId(null)
                .biometricType(null)
                .deviceId(null)
                .biometricHash(null)
                .timestamp(null)
                .build();

        // when
        Set<ConstraintViolation<BiometricLoginRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(5); // employeeId, biometricType, deviceId, biometricHash, timestamp
    }

    @Test
    @DisplayName("BiometricLoginRequestDto 유효한 사원번호 길이 범위 테스트")
    void testValidEmployeeIdSizeRange() {
        // given & when & then
        String[] validEmployeeIds = {"CHN0", "CHN01", "CHN001", "CHN0001234"};
        
        for (String employeeId : validEmployeeIds) {
            BiometricLoginRequestDto dto = BiometricLoginRequestDto.builder()
                    .employeeId(employeeId)
                    .biometricType("FINGERPRINT")
                    .deviceId("device123")
                    .biometricHash("hash123")
                    .timestamp(System.currentTimeMillis())
                    .build();

            Set<ConstraintViolation<BiometricLoginRequestDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("BiometricLoginRequestDto 타임스탬프 유효 범위 테스트")
    void testValidTimestampRange() {
        // given
        Long validTimestamp = 1000000000000L; // 최소 유효 타임스탬프
        Long currentTimestamp = System.currentTimeMillis();

        // when
        BiometricLoginRequestDto minValidDto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FINGERPRINT")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(validTimestamp)
                .build();

        BiometricLoginRequestDto currentDto = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FINGERPRINT")
                .deviceId("device123")
                .biometricHash("hash123")
                .timestamp(currentTimestamp)
                .build();

        // then
        Set<ConstraintViolation<BiometricLoginRequestDto>> minValidViolations = validator.validate(minValidDto);
        Set<ConstraintViolation<BiometricLoginRequestDto>> currentViolations = validator.validate(currentDto);

        assertThat(minValidViolations).isEmpty();
        assertThat(currentViolations).isEmpty();
    }
}
