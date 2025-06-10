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

class LoginRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("LoginRequestDto 유효한 데이터 생성 테스트")
    void createValidLoginRequestDto() {
        // given
        String employeeId = "CHN0001";
        String password = "password123";

        // when
        LoginRequestDto loginRequestDto = LoginRequestDto.builder()
                .employeeId(employeeId)
                .password(password)
                .build();

        // then
        assertThat(loginRequestDto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(loginRequestDto.getPassword()).isEqualTo(password);
        
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(loginRequestDto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("LoginRequestDto employeeId null 검증 테스트")
    void validateEmployeeIdNotNull() {
        // given
        LoginRequestDto loginRequestDto = LoginRequestDto.builder()
                .employeeId(null)
                .password("password123")
                .build();

        // when
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(loginRequestDto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<LoginRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("사원번호가 null 입니다");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("employeeId");
    }

    @Test
    @DisplayName("LoginRequestDto password null 검증 테스트")
    void validatePasswordNotNull() {
        // given
        LoginRequestDto loginRequestDto = LoginRequestDto.builder()
                .employeeId("CHN0001")
                .password(null)
                .build();

        // when
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(loginRequestDto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<LoginRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("비밀번호를 입력해 주세요.");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
    }

    @Test
    @DisplayName("LoginRequestDto 모든 필드 null 검증 테스트")
    void validateAllFieldsNull() {
        // given
        LoginRequestDto loginRequestDto = LoginRequestDto.builder()
                .employeeId(null)
                .password(null)
                .build();

        // when
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(loginRequestDto);

        // then
        assertThat(violations).hasSize(2);
    }

    @Test
    @DisplayName("LoginRequestDto 기본 생성자 테스트")
    void createLoginRequestDtoWithNoArgsConstructor() {
        // when
        LoginRequestDto loginRequestDto = new LoginRequestDto();

        // then
        assertThat(loginRequestDto).isNotNull();
        assertThat(loginRequestDto.getEmployeeId()).isNull();
        assertThat(loginRequestDto.getPassword()).isNull();
    }

    @Test
    @DisplayName("LoginRequestDto 전체 생성자 테스트")
    void createLoginRequestDtoWithAllArgsConstructor() {
        // given
        String employeeId = "CHN0001";
        String password = "mypassword";

        // when
        LoginRequestDto loginRequestDto = new LoginRequestDto(employeeId, password);

        // then
        assertThat(loginRequestDto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(loginRequestDto.getPassword()).isEqualTo(password);
    }

    @Test
    @DisplayName("LoginRequestDto Setter 테스트")
    void testLoginRequestDtoSetter() {
        // given
        LoginRequestDto loginRequestDto = new LoginRequestDto();
        String employeeId = "CHN0001";
        String password = "newpassword";

        // when
        loginRequestDto.setEmployeeId(employeeId);
        loginRequestDto.setPassword(password);

        // then
        assertThat(loginRequestDto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(loginRequestDto.getPassword()).isEqualTo(password);
    }

    @Test
    @DisplayName("LoginRequestDto 빈 문자열 허용 테스트")
    void testLoginRequestDtoWithEmptyStrings() {
        // given
        LoginRequestDto loginRequestDto = LoginRequestDto.builder()
                .employeeId("")
                .password("")
                .build();

        // when
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(loginRequestDto);

        // then
        // @NotNull은 빈 문자열을 허용하므로 검증 오류가 없어야 함
        assertThat(violations).isEmpty();
    }
}
