package kr.ssok.ssom.backend.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AttemptResult Enum 단위 테스트")
class AttemptResultTest {

    @Test
    @DisplayName("AttemptResult Enum은 3개의 값을 가진다")
    void testAttemptResultEnumValues() {
        // Given & When
        AttemptResult[] attemptResults = AttemptResult.values();

        // Then
        assertThat(attemptResults).hasSize(3);
        assertThat(attemptResults).containsExactlyInAnyOrder(
                AttemptResult.SUCCESS,
                AttemptResult.FAILED,
                AttemptResult.BLOCKED
        );
    }

    @ParameterizedTest
    @CsvSource({
            "SUCCESS, 성공",
            "FAILED, 실패",
            "BLOCKED, 차단됨"
    })
    @DisplayName("각 AttemptResult는 올바른 description을 가진다")
    void testAttemptResultDescription(String attemptResultName, String expectedDescription) {
        // Given
        AttemptResult attemptResult = AttemptResult.valueOf(attemptResultName);

        // When & Then
        assertThat(attemptResult.getDescription()).isEqualTo(expectedDescription);
    }

    @Test
    @DisplayName("AttemptResult는 toString 메서드가 올바르게 동작한다")
    void testAttemptResultToString() {
        // Given & When & Then
        assertThat(AttemptResult.SUCCESS.toString()).isEqualTo("SUCCESS");
        assertThat(AttemptResult.FAILED.toString()).isEqualTo("FAILED");
        assertThat(AttemptResult.BLOCKED.toString()).isEqualTo("BLOCKED");
    }

    @Test
    @DisplayName("AttemptResult는 ordinal 메서드가 올바르게 동작한다")
    void testAttemptResultOrdinal() {
        // Given & When & Then
        assertThat(AttemptResult.SUCCESS.ordinal()).isEqualTo(0);
        assertThat(AttemptResult.FAILED.ordinal()).isEqualTo(1);
        assertThat(AttemptResult.BLOCKED.ordinal()).isEqualTo(2);
    }

    @Test
    @DisplayName("AttemptResult는 name 메서드가 올바르게 동작한다")
    void testAttemptResultName() {
        // Given & When & Then
        assertThat(AttemptResult.SUCCESS.name()).isEqualTo("SUCCESS");
        assertThat(AttemptResult.FAILED.name()).isEqualTo("FAILED");
        assertThat(AttemptResult.BLOCKED.name()).isEqualTo("BLOCKED");
    }

    @Test
    @DisplayName("AttemptResult는 valueOf 메서드가 올바르게 동작한다")
    void testAttemptResultValueOf() {
        // Given & When & Then
        assertThat(AttemptResult.valueOf("SUCCESS")).isEqualTo(AttemptResult.SUCCESS);
        assertThat(AttemptResult.valueOf("FAILED")).isEqualTo(AttemptResult.FAILED);
        assertThat(AttemptResult.valueOf("BLOCKED")).isEqualTo(AttemptResult.BLOCKED);
    }

    @Test
    @DisplayName("AttemptResult는 잘못된 값에 대해 valueOf에서 예외를 발생시킨다")
    void testAttemptResultValueOfThrowsExceptionForInvalidValue() {
        // When & Then
        assertThatThrownBy(() -> AttemptResult.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("각 AttemptResult의 description은 고유하다")
    void testAttemptResultDescriptionsAreUnique() {
        // Given
        AttemptResult[] attemptResults = AttemptResult.values();

        // When
        String[] descriptions = new String[attemptResults.length];
        for (int i = 0; i < attemptResults.length; i++) {
            descriptions[i] = attemptResults[i].getDescription();
        }

        // Then
        assertThat(descriptions).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("AttemptResult는 equals와 hashCode가 올바르게 동작한다")
    void testAttemptResultEqualsAndHashCode() {
        // Given
        AttemptResult success1 = AttemptResult.SUCCESS;
        AttemptResult success2 = AttemptResult.valueOf("SUCCESS");
        AttemptResult failed = AttemptResult.FAILED;

        // When & Then
        assertThat(success1).isEqualTo(success2);
        assertThat(success1).isNotEqualTo(failed);
        assertThat(success1.hashCode()).isEqualTo(success2.hashCode());
    }

    @Test
    @DisplayName("AttemptResult Enum은 null과 비교할 수 있다")
    void testAttemptResultNullComparison() {
        // Given
        AttemptResult attemptResult = AttemptResult.SUCCESS;

        // When & Then
        assertThat(attemptResult).isNotNull();
        assertThat(attemptResult).isNotEqualTo(null);
    }

    @Test
    @DisplayName("SUCCESS는 성공적인 시도를 나타낸다")
    void testSuccessRepresentsSuccessfulAttempt() {
        // Given & When
        AttemptResult success = AttemptResult.SUCCESS;

        // Then
        assertThat(success.getDescription()).isEqualTo("성공");
        assertThat(success.name()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("FAILED는 실패한 시도를 나타낸다")
    void testFailedRepresentsFailedAttempt() {
        // Given & When
        AttemptResult failed = AttemptResult.FAILED;

        // Then
        assertThat(failed.getDescription()).isEqualTo("실패");
        assertThat(failed.name()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("BLOCKED는 차단된 시도를 나타낸다")
    void testBlockedRepresentsBlockedAttempt() {
        // Given & When
        AttemptResult blocked = AttemptResult.BLOCKED;

        // Then
        assertThat(blocked.getDescription()).isEqualTo("차단됨");
        assertThat(blocked.name()).isEqualTo("BLOCKED");
    }
}
