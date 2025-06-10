package kr.ssok.ssom.backend.domain.user.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BiometricStatusDtoTest {

    @Test
    @DisplayName("BiometricStatusDto 생성 테스트")
    void createBiometricStatusDto() {
        // given
        Boolean isRegistered = true;
        List<String> availableTypes = Arrays.asList("FINGERPRINT", "FACE");
        Integer deviceCount = 2;
        LocalDateTime lastUsedAt = LocalDateTime.of(2024, 6, 10, 14, 30, 0);

        // when
        BiometricStatusDto dto = BiometricStatusDto.builder()
                .isRegistered(isRegistered)
                .availableTypes(availableTypes)
                .deviceCount(deviceCount)
                .lastUsedAt(lastUsedAt)
                .build();

        // then
        assertThat(dto.getIsRegistered()).isTrue();
        assertThat(dto.getAvailableTypes()).hasSize(2);
        assertThat(dto.getAvailableTypes()).contains("FINGERPRINT", "FACE");
        assertThat(dto.getDeviceCount()).isEqualTo(2);
        assertThat(dto.getLastUsedAt()).isEqualTo(lastUsedAt);
    }

    @Test
    @DisplayName("BiometricStatusDto 기본 생성자 테스트")
    void createBiometricStatusDtoWithNoArgsConstructor() {
        // when
        BiometricStatusDto dto = new BiometricStatusDto();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getIsRegistered()).isNull();
        assertThat(dto.getAvailableTypes()).isNull();
        assertThat(dto.getDeviceCount()).isNull();
        assertThat(dto.getLastUsedAt()).isNull();
    }

    @Test
    @DisplayName("BiometricStatusDto 전체 생성자 테스트")
    void createBiometricStatusDtoWithAllArgsConstructor() {
        // given
        Boolean isRegistered = false;
        List<String> availableTypes = Collections.emptyList();
        Integer deviceCount = 0;
        LocalDateTime lastUsedAt = null;

        // when
        BiometricStatusDto dto = new BiometricStatusDto(
                isRegistered, availableTypes, deviceCount, lastUsedAt
        );

        // then
        assertThat(dto.getIsRegistered()).isFalse();
        assertThat(dto.getAvailableTypes()).isEmpty();
        assertThat(dto.getDeviceCount()).isEqualTo(0);
        assertThat(dto.getLastUsedAt()).isNull();
    }

    @Test
    @DisplayName("BiometricStatusDto Setter 테스트")
    void testBiometricStatusDtoSetter() {
        // given
        BiometricStatusDto dto = new BiometricStatusDto();
        Boolean isRegistered = true;
        List<String> availableTypes = Arrays.asList("FINGERPRINT");
        Integer deviceCount = 1;
        LocalDateTime lastUsedAt = LocalDateTime.now();

        // when
        dto.setIsRegistered(isRegistered);
        dto.setAvailableTypes(availableTypes);
        dto.setDeviceCount(deviceCount);
        dto.setLastUsedAt(lastUsedAt);

        // then
        assertThat(dto.getIsRegistered()).isTrue();
        assertThat(dto.getAvailableTypes()).hasSize(1);
        assertThat(dto.getAvailableTypes()).contains("FINGERPRINT");
        assertThat(dto.getDeviceCount()).isEqualTo(1);
        assertThat(dto.getLastUsedAt()).isEqualTo(lastUsedAt);
    }

    @Test
    @DisplayName("BiometricStatusDto 생체인증 미등록 상태 테스트")
    void testUnregisteredBiometricStatus() {
        // when
        BiometricStatusDto dto = BiometricStatusDto.builder()
                .isRegistered(false)
                .availableTypes(Collections.emptyList())
                .deviceCount(0)
                .lastUsedAt(null)
                .build();

        // then
        assertThat(dto.getIsRegistered()).isFalse();
        assertThat(dto.getAvailableTypes()).isEmpty();
        assertThat(dto.getDeviceCount()).isEqualTo(0);
        assertThat(dto.getLastUsedAt()).isNull();
    }

    @Test
    @DisplayName("BiometricStatusDto 다중 생체인증 타입 등록 상태 테스트")
    void testMultipleBiometricTypesRegistered() {
        // given
        List<String> availableTypes = Arrays.asList("FINGERPRINT", "FACE");
        LocalDateTime lastUsed = LocalDateTime.of(2024, 6, 10, 16, 45, 0);

        // when
        BiometricStatusDto dto = BiometricStatusDto.builder()
                .isRegistered(true)
                .availableTypes(availableTypes)
                .deviceCount(3)
                .lastUsedAt(lastUsed)
                .build();

        // then
        assertThat(dto.getIsRegistered()).isTrue();
        assertThat(dto.getAvailableTypes()).hasSize(2);
        assertThat(dto.getAvailableTypes()).containsExactlyInAnyOrder("FINGERPRINT", "FACE");
        assertThat(dto.getDeviceCount()).isEqualTo(3);
        assertThat(dto.getLastUsedAt()).isEqualTo(lastUsed);
    }
}
