package kr.ssok.ssom.backend.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BiometricType Enum 단위 테스트")
class BiometricTypeTest {

    @Test
    @DisplayName("BiometricType Enum은 2개의 값을 가진다")
    void testBiometricTypeEnumValues() {
        // Given & When
        BiometricType[] biometricTypes = BiometricType.values();

        // Then
        assertThat(biometricTypes).hasSize(2);
        assertThat(biometricTypes).containsExactlyInAnyOrder(
                BiometricType.FINGERPRINT,
                BiometricType.FACE
        );
    }

    @ParameterizedTest
    @CsvSource({
            "FINGERPRINT, 지문",
            "FACE, 얼굴"
    })
    @DisplayName("각 BiometricType은 올바른 description을 가진다")
    void testBiometricTypeDescription(String biometricTypeName, String expectedDescription) {
        // Given
        BiometricType biometricType = BiometricType.valueOf(biometricTypeName);

        // When & Then
        assertThat(biometricType.getDescription()).isEqualTo(expectedDescription);
    }

    @Test
    @DisplayName("BiometricType은 toString 메서드가 올바르게 동작한다")
    void testBiometricTypeToString() {
        // Given & When & Then
        assertThat(BiometricType.FINGERPRINT.toString()).isEqualTo("FINGERPRINT");
        assertThat(BiometricType.FACE.toString()).isEqualTo("FACE");
    }

    @Test
    @DisplayName("BiometricType은 ordinal 메서드가 올바르게 동작한다")
    void testBiometricTypeOrdinal() {
        // Given & When & Then
        assertThat(BiometricType.FINGERPRINT.ordinal()).isEqualTo(0);
        assertThat(BiometricType.FACE.ordinal()).isEqualTo(1);
    }

    @Test
    @DisplayName("BiometricType은 name 메서드가 올바르게 동작한다")
    void testBiometricTypeName() {
        // Given & When & Then
        assertThat(BiometricType.FINGERPRINT.name()).isEqualTo("FINGERPRINT");
        assertThat(BiometricType.FACE.name()).isEqualTo("FACE");
    }

    @Test
    @DisplayName("BiometricType은 valueOf 메서드가 올바르게 동작한다")
    void testBiometricTypeValueOf() {
        // Given & When & Then
        assertThat(BiometricType.valueOf("FINGERPRINT")).isEqualTo(BiometricType.FINGERPRINT);
        assertThat(BiometricType.valueOf("FACE")).isEqualTo(BiometricType.FACE);
    }

    @Test
    @DisplayName("BiometricType은 잘못된 값에 대해 valueOf에서 예외를 발생시킨다")
    void testBiometricTypeValueOfThrowsExceptionForInvalidValue() {
        // When & Then
        assertThatThrownBy(() -> BiometricType.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("각 BiometricType의 description은 고유하다")
    void testBiometricTypeDescriptionsAreUnique() {
        // Given
        BiometricType[] biometricTypes = BiometricType.values();

        // When
        String[] descriptions = new String[biometricTypes.length];
        for (int i = 0; i < biometricTypes.length; i++) {
            descriptions[i] = biometricTypes[i].getDescription();
        }

        // Then
        assertThat(descriptions).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("BiometricType은 equals와 hashCode가 올바르게 동작한다")
    void testBiometricTypeEqualsAndHashCode() {
        // Given
        BiometricType fingerprint1 = BiometricType.FINGERPRINT;
        BiometricType fingerprint2 = BiometricType.valueOf("FINGERPRINT");
        BiometricType face = BiometricType.FACE;

        // When & Then
        assertThat(fingerprint1).isEqualTo(fingerprint2);
        assertThat(fingerprint1).isNotEqualTo(face);
        assertThat(fingerprint1.hashCode()).isEqualTo(fingerprint2.hashCode());
    }

    @Test
    @DisplayName("BiometricType Enum은 null과 비교할 수 있다")
    void testBiometricTypeNullComparison() {
        // Given
        BiometricType biometricType = BiometricType.FINGERPRINT;

        // When & Then
        assertThat(biometricType).isNotNull();
        assertThat(biometricType).isNotEqualTo(null);
    }
}
