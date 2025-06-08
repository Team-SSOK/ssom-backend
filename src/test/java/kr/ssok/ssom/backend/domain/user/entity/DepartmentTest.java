package kr.ssok.ssom.backend.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Department Enum 단위 테스트")
class DepartmentTest {

    @Test
    @DisplayName("Department Enum은 4개의 값을 가진다")
    void testDepartmentEnumValues() {
        // Given & When
        Department[] departments = Department.values();

        // Then
        assertThat(departments).hasSize(4);
        assertThat(departments).containsExactlyInAnyOrder(
                Department.CHANNEL,
                Department.CORE_BANK,
                Department.EXTERNAL,
                Department.OPERATION
        );
    }

    @ParameterizedTest
    @CsvSource({
            "CHANNEL, 1, CHN",
            "CORE_BANK, 2, CORE",
            "EXTERNAL, 3, EXT",
            "OPERATION, 4, OPR"
    })
    @DisplayName("각 Department는 올바른 code와 prefix를 가진다")
    void testDepartmentCodeAndPrefix(String departmentName, int expectedCode, String expectedPrefix) {
        // Given
        Department department = Department.valueOf(departmentName);

        // When & Then
        assertThat(department.getCode()).isEqualTo(expectedCode);
        assertThat(department.getPrefix()).isEqualTo(expectedPrefix);
    }

    @ParameterizedTest
    @CsvSource({
            "1, CHANNEL",
            "2, CORE_BANK",
            "3, EXTERNAL",
            "4, OPERATION"
    })
    @DisplayName("fromCode 메서드는 code로 올바른 Department를 반환한다")
    void testFromCodeReturnsCorrectDepartment(int code, String expectedDepartmentName) {
        // When
        Department department = Department.fromCode(code);

        // Then
        assertThat(department).isEqualTo(Department.valueOf(expectedDepartmentName));
        assertThat(department.getCode()).isEqualTo(code);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, -1, 100})
    @DisplayName("fromCode 메서드는 잘못된 code에 대해 예외를 발생시킨다")
    void testFromCodeThrowsExceptionForInvalidCode(int invalidCode) {
        // When & Then
        assertThatThrownBy(() -> Department.fromCode(invalidCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid department code: " + invalidCode);
    }

    @ParameterizedTest
    @CsvSource({
            "CHANNEL, CHANNEL",
            "CORE_BANK, CORE_BANK",
            "EXTERNAL, EXTERNAL",
            "OPERATION, OPERATION",
            "channel, CHANNEL",
            "core_bank, CORE_BANK",
            "external, EXTERNAL",
            "operation, OPERATION"
    })
    @DisplayName("fromName 메서드는 name으로 올바른 Department를 반환한다")
    void testFromNameReturnsCorrectDepartment(String name, String expectedDepartmentName) {
        // When
        Department department = Department.fromName(name);

        // Then
        assertThat(department).isEqualTo(Department.valueOf(expectedDepartmentName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "test", "", "NULL", "department"})
    @DisplayName("fromName 메서드는 잘못된 name에 대해 예외를 발생시킨다")
    void testFromNameThrowsExceptionForInvalidName(String invalidName) {
        // When & Then
        assertThatThrownBy(() -> Department.fromName(invalidName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid department name: " + invalidName);
    }

    @Test
    @DisplayName("Department는 toString 메서드가 올바르게 동작한다")
    void testDepartmentToString() {
        // Given & When & Then
        assertThat(Department.CHANNEL.toString()).isEqualTo("CHANNEL");
        assertThat(Department.CORE_BANK.toString()).isEqualTo("CORE_BANK");
        assertThat(Department.EXTERNAL.toString()).isEqualTo("EXTERNAL");
        assertThat(Department.OPERATION.toString()).isEqualTo("OPERATION");
    }

    @Test
    @DisplayName("Department는 ordinal 메서드가 올바르게 동작한다")
    void testDepartmentOrdinal() {
        // Given & When & Then
        assertThat(Department.CHANNEL.ordinal()).isEqualTo(0);
        assertThat(Department.CORE_BANK.ordinal()).isEqualTo(1);
        assertThat(Department.EXTERNAL.ordinal()).isEqualTo(2);
        assertThat(Department.OPERATION.ordinal()).isEqualTo(3);
    }

    @Test
    @DisplayName("Department는 name 메서드가 올바르게 동작한다")
    void testDepartmentName() {
        // Given & When & Then
        assertThat(Department.CHANNEL.name()).isEqualTo("CHANNEL");
        assertThat(Department.CORE_BANK.name()).isEqualTo("CORE_BANK");
        assertThat(Department.EXTERNAL.name()).isEqualTo("EXTERNAL");
        assertThat(Department.OPERATION.name()).isEqualTo("OPERATION");
    }

    @Test
    @DisplayName("각 Department의 prefix는 고유하다")
    void testDepartmentPrefixesAreUnique() {
        // Given
        Department[] departments = Department.values();

        // When
        String[] prefixes = new String[departments.length];
        for (int i = 0; i < departments.length; i++) {
            prefixes[i] = departments[i].getPrefix();
        }

        // Then
        assertThat(prefixes).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("각 Department의 code는 고유하다")
    void testDepartmentCodesAreUnique() {
        // Given
        Department[] departments = Department.values();

        // When
        int[] codes = new int[departments.length];
        for (int i = 0; i < departments.length; i++) {
            codes[i] = departments[i].getCode();
        }

        // Then
        assertThat(codes).doesNotHaveDuplicates();
    }
}
