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

class SignupRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("SignupRequestDto 유효한 데이터 생성 테스트")
    void createValidSignupRequestDto() {
        // given
        String username = "홍길동";
        String password = "password123";
        String phoneNumber = "010-1234-5678";
        int departmentCode = 1;
        String githubId = "hong-gildong";

        // when
        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .username(username)
                .password(password)
                .phoneNumber(phoneNumber)
                .departmentCode(departmentCode)
                .githubId(githubId)
                .build();

        // then
        assertThat(signupRequestDto.getUsername()).isEqualTo(username);
        assertThat(signupRequestDto.getPassword()).isEqualTo(password);
        assertThat(signupRequestDto.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(signupRequestDto.getDepartmentCode()).isEqualTo(departmentCode);
        assertThat(signupRequestDto.getGithubId()).isEqualTo(githubId);

        Set<ConstraintViolation<SignupRequestDto>> violations = validator.validate(signupRequestDto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("SignupRequestDto username 공백 검증 테스트")
    void validateUsernameNotBlank() {
        // given
        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .username("")
                .password("password123")
                .phoneNumber("010-1234-5678")
                .departmentCode(1)
                .build();

        // when
        Set<ConstraintViolation<SignupRequestDto>> violations = validator.validate(signupRequestDto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<SignupRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("사용자명은 필수입니다");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("username");
    }

    @Test
    @DisplayName("SignupRequestDto password 공백 검증 테스트")
    void validatePasswordNotBlank() {
        // given
        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .username("홍길동")
                .password("")
                .phoneNumber("010-1234-5678")
                .departmentCode(1)
                .build();

        // when
        Set<ConstraintViolation<SignupRequestDto>> violations = validator.validate(signupRequestDto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<SignupRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("비밀번호는 필수입니다");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
    }

    @Test
    @DisplayName("SignupRequestDto phoneNumber 공백 검증 테스트")
    void validatePhoneNumberNotBlank() {
        // given
        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .username("홍길동")
                .password("password123")
                .phoneNumber("")
                .departmentCode(1)
                .build();

        // when
        Set<ConstraintViolation<SignupRequestDto>> violations = validator.validate(signupRequestDto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<SignupRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("전화번호는 필수입니다");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("phoneNumber");
    }

    @Test
    @DisplayName("SignupRequestDto departmentCode 최소값 검증 테스트")
    void validateDepartmentCodeMin() {
        // given
        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .username("홍길동")
                .password("password123")
                .phoneNumber("010-1234-5678")
                .departmentCode(0)
                .build();

        // when
        Set<ConstraintViolation<SignupRequestDto>> violations = validator.validate(signupRequestDto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<SignupRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("부서코드는 1 이상이어야 합니다");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("departmentCode");
    }

    @Test
    @DisplayName("SignupRequestDto departmentCode 최대값 검증 테스트")
    void validateDepartmentCodeMax() {
        // given
        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .username("홍길동")
                .password("password123")
                .phoneNumber("010-1234-5678")
                .departmentCode(5)
                .build();

        // when
        Set<ConstraintViolation<SignupRequestDto>> violations = validator.validate(signupRequestDto);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<SignupRequestDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("부서코드는 4 이하여야 합니다");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("departmentCode");
    }

    @Test
    @DisplayName("SignupRequestDto departmentCode 유효한 범위 테스트")
    void validateDepartmentCodeValidRange() {
        // given & when & then
        for (int i = 1; i <= 4; i++) {
            SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                    .username("홍길동")
                    .password("password123")
                    .phoneNumber("010-1234-5678")
                    .departmentCode(i)
                    .build();

            Set<ConstraintViolation<SignupRequestDto>> violations = validator.validate(signupRequestDto);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("SignupRequestDto null 필드 검증 테스트")
    void validateNullFields() {
        // given
        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .username(null)
                .password(null)
                .phoneNumber(null)
                .departmentCode(1)
                .githubId(null)
                .build();

        // when
        Set<ConstraintViolation<SignupRequestDto>> violations = validator.validate(signupRequestDto);

        // then
        assertThat(violations).hasSize(3); // username, password, phoneNumber
    }

    @Test
    @DisplayName("SignupRequestDto 기본 생성자 테스트")
    void createSignupRequestDtoWithNoArgsConstructor() {
        // when
        SignupRequestDto signupRequestDto = new SignupRequestDto();

        // then
        assertThat(signupRequestDto).isNotNull();
        assertThat(signupRequestDto.getUsername()).isNull();
        assertThat(signupRequestDto.getPassword()).isNull();
        assertThat(signupRequestDto.getPhoneNumber()).isNull();
        assertThat(signupRequestDto.getDepartmentCode()).isEqualTo(0);
        assertThat(signupRequestDto.getGithubId()).isNull();
    }

    @Test
    @DisplayName("SignupRequestDto 전체 생성자 테스트")
    void createSignupRequestDtoWithAllArgsConstructor() {
        // given
        String username = "김철수";
        String password = "mypassword";
        String phoneNumber = "010-5678-1234";
        int departmentCode = 2;
        String githubId = "kim-cheolsu";

        // when
        SignupRequestDto signupRequestDto = new SignupRequestDto(
                username, password, phoneNumber, departmentCode, githubId
        );

        // then
        assertThat(signupRequestDto.getUsername()).isEqualTo(username);
        assertThat(signupRequestDto.getPassword()).isEqualTo(password);
        assertThat(signupRequestDto.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(signupRequestDto.getDepartmentCode()).isEqualTo(departmentCode);
        assertThat(signupRequestDto.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("SignupRequestDto Setter 테스트")
    void testSignupRequestDtoSetter() {
        // given
        SignupRequestDto signupRequestDto = new SignupRequestDto();
        String username = "이영희";
        String password = "newpassword";
        String phoneNumber = "010-9999-8888";
        int departmentCode = 3;
        String githubId = "lee-younghee";

        // when
        signupRequestDto.setUsername(username);
        signupRequestDto.setPassword(password);
        signupRequestDto.setPhoneNumber(phoneNumber);
        signupRequestDto.setDepartmentCode(departmentCode);
        signupRequestDto.setGithubId(githubId);

        // then
        assertThat(signupRequestDto.getUsername()).isEqualTo(username);
        assertThat(signupRequestDto.getPassword()).isEqualTo(password);
        assertThat(signupRequestDto.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(signupRequestDto.getDepartmentCode()).isEqualTo(departmentCode);
        assertThat(signupRequestDto.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("SignupRequestDto githubId 선택적 필드 테스트")
    void testOptionalGithubId() {
        // given & when
        SignupRequestDto signupRequestDtoWithGithub = SignupRequestDto.builder()
                .username("홍길동")
                .password("password123")
                .phoneNumber("010-1234-5678")
                .departmentCode(1)
                .githubId("hong-gildong")
                .build();

        SignupRequestDto signupRequestDtoWithoutGithub = SignupRequestDto.builder()
                .username("김철수")
                .password("password456")
                .phoneNumber("010-5678-1234")
                .departmentCode(2)
                .githubId(null)
                .build();

        // then
        Set<ConstraintViolation<SignupRequestDto>> violationsWithGithub = 
                validator.validate(signupRequestDtoWithGithub);
        Set<ConstraintViolation<SignupRequestDto>> violationsWithoutGithub = 
                validator.validate(signupRequestDtoWithoutGithub);

        assertThat(violationsWithGithub).isEmpty();
        assertThat(violationsWithoutGithub).isEmpty();
        assertThat(signupRequestDtoWithGithub.getGithubId()).isEqualTo("hong-gildong");
        assertThat(signupRequestDtoWithoutGithub.getGithubId()).isNull();
    }

    @Test
    @DisplayName("SignupRequestDto JsonProperty 어노테이션 필드 테스트")
    void testJsonPropertyFields() {
        // given
        String phoneNumber = "010-1111-2222";
        int departmentCode = 4;
        String githubId = "test-user";

        // when
        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .username("테스트사용자")
                .password("testpassword")
                .phoneNumber(phoneNumber)
                .departmentCode(departmentCode)
                .githubId(githubId)
                .build();

        // then
        // JsonProperty 어노테이션이 붙은 필드들이 정상적으로 설정되는지 확인
        assertThat(signupRequestDto.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(signupRequestDto.getDepartmentCode()).isEqualTo(departmentCode);
        assertThat(signupRequestDto.getGithubId()).isEqualTo(githubId);
    }
}
