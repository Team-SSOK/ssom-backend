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

class PasswordChangeRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("PasswordChangeRequestDto 유효한 데이터 생성 테스트")
    void createValidPasswordChangeRequestDto() {
        // given
        String currentPassword = "oldPassword123!";
        String newPassword = "newPassword456@";
        String confirmPassword = "newPassword456@";

        // when
        PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmPassword(confirmPassword)
                .build();

        // then
        assertThat(dto.getCurrentPassword()).isEqualTo(currentPassword);
        assertThat(dto.getNewPassword()).isEqualTo(newPassword);
        assertThat(dto.getConfirmPassword()).isEqualTo(confirmPassword);

        Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PasswordChangeRequestDto currentPassword NotBlank 검증 테스트")
    void validateCurrentPasswordNotBlank() {
        // given
        PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                .currentPassword("")
                .newPassword("newPassword123!")
                .confirmPassword("newPassword123!")
                .build();

        // when
        Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<PasswordChangeRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("현재 비밀번호를 입력해주세요.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("currentPassword");
    }

    @Test
    @DisplayName("PasswordChangeRequestDto newPassword NotBlank 검증 테스트")
    void validateNewPasswordNotBlank() {
        // given
        PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                .currentPassword("oldPassword123!")
                .newPassword("")
                .confirmPassword("newPassword123!")
                .build();

        // when
        Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(2); // NotBlank + Pattern validation 모두 발생
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "새 비밀번호를 입력해주세요.",
                        "비밀번호는 최소 8자리이며, 영문자, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다."
                );
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .allMatch(path -> path.equals("newPassword"));
    }

    @Test
    @DisplayName("PasswordChangeRequestDto confirmPassword NotBlank 검증 테스트")
    void validateConfirmPasswordNotBlank() {
        // given
        PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                .currentPassword("oldPassword123!")
                .newPassword("newPassword123!")
                .confirmPassword("")
                .build();

        // when
        Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<PasswordChangeRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("새 비밀번호 확인을 입력해주세요.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("confirmPassword");
    }

    @Test
    @DisplayName("PasswordChangeRequestDto newPassword Pattern 검증 테스트 - 유효한 패스워드")
    void validateNewPasswordPatternValid() {
        // given
        String[] validPasswords = {
                "Password1!",
                "MySecret123@",
                "Test1234#",
                "ValidPass5$",
                "Secure999%"
        };

        for (String password : validPasswords) {
            PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                    .currentPassword("oldPassword123!")
                    .newPassword(password)
                    .confirmPassword(password)
                    .build();

            // when
            Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("PasswordChangeRequestDto newPassword Pattern 검증 테스트 - 영문자 없음")
    void validateNewPasswordPatternNoAlphabet() {
        // given
        PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                .currentPassword("oldPassword123!")
                .newPassword("12345678!")
                .confirmPassword("12345678!")
                .build();

        // when
        Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<PasswordChangeRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("비밀번호는 최소 8자리이며, 영문자, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("PasswordChangeRequestDto newPassword Pattern 검증 테스트 - 숫자 없음")
    void validateNewPasswordPatternNoDigit() {
        // given
        PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                .currentPassword("oldPassword123!")
                .newPassword("Password!")
                .confirmPassword("Password!")
                .build();

        // when
        Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<PasswordChangeRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("비밀번호는 최소 8자리이며, 영문자, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("PasswordChangeRequestDto newPassword Pattern 검증 테스트 - 특수문자 없음")
    void validateNewPasswordPatternNoSpecialChar() {
        // given
        PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                .currentPassword("oldPassword123!")
                .newPassword("Password123")
                .confirmPassword("Password123")
                .build();

        // when
        Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<PasswordChangeRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("비밀번호는 최소 8자리이며, 영문자, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("PasswordChangeRequestDto newPassword Pattern 검증 테스트 - 길이 부족")
    void validateNewPasswordPatternTooShort() {
        // given
        PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                .currentPassword("oldPassword123!")
                .newPassword("Pass1!")
                .confirmPassword("Pass1!")
                .build();

        // when
        Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<PasswordChangeRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("비밀번호는 최소 8자리이며, 영문자, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("PasswordChangeRequestDto 기본 생성자 테스트")
    void createPasswordChangeRequestDtoWithNoArgsConstructor() {
        // when
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getCurrentPassword()).isNull();
        assertThat(dto.getNewPassword()).isNull();
        assertThat(dto.getConfirmPassword()).isNull();
    }

    @Test
    @DisplayName("PasswordChangeRequestDto 전체 생성자 테스트")
    void createPasswordChangeRequestDtoWithAllArgsConstructor() {
        // given
        String currentPassword = "currentPass123!";
        String newPassword = "newPassword456@";
        String confirmPassword = "newPassword456@";

        // when
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto(
                currentPassword, newPassword, confirmPassword
        );

        // then
        assertThat(dto.getCurrentPassword()).isEqualTo(currentPassword);
        assertThat(dto.getNewPassword()).isEqualTo(newPassword);
        assertThat(dto.getConfirmPassword()).isEqualTo(confirmPassword);
    }

    @Test
    @DisplayName("PasswordChangeRequestDto Setter 테스트")
    void testPasswordChangeRequestDtoSetter() {
        // given
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto();
        String currentPassword = "setterCurrent123!";
        String newPassword = "setterNew456@";
        String confirmPassword = "setterNew456@";

        // when
        dto.setCurrentPassword(currentPassword);
        dto.setNewPassword(newPassword);
        dto.setConfirmPassword(confirmPassword);

        // then
        assertThat(dto.getCurrentPassword()).isEqualTo(currentPassword);
        assertThat(dto.getNewPassword()).isEqualTo(newPassword);
        assertThat(dto.getConfirmPassword()).isEqualTo(confirmPassword);
    }

    @Test
    @DisplayName("PasswordChangeRequestDto 모든 필드 null 검증 테스트")
    void validateAllFieldsNull() {
        // given
        PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                .currentPassword(null)
                .newPassword(null)
                .confirmPassword(null)
                .build();

        // when
        Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(3); // currentPassword, newPassword, confirmPassword
    }

    @Test
    @DisplayName("PasswordChangeRequestDto 지원되는 특수문자 테스트")
    void testSupportedSpecialCharacters() {
        // given
        String[] specialChars = {"@", "$", "!", "%", "*", "#", "?", "&"};
        
        for (String specialChar : specialChars) {
            String password = "Password1" + specialChar;
            PasswordChangeRequestDto dto = PasswordChangeRequestDto.builder()
                    .currentPassword("oldPassword123!")
                    .newPassword(password)
                    .confirmPassword(password)
                    .build();

            // when
            Set<ConstraintViolation<PasswordChangeRequestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }
    }
}
