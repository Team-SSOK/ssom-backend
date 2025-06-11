package kr.ssok.ssom.backend.domain.user.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class UserResponseDtoTest {

    @Test
    @DisplayName("UserResponseDto 생성 테스트")
    void createUserResponseDto() {
        // given
        String employeeId = "CHN0001";
        String username = "홍길동";
        String phoneNumber = "010-1234-5678";
        String department = "CHANNEL";
        int departmentCode = 1;
        String githubId = "hong-gildong";

        // when
        UserResponseDto userResponseDto = UserResponseDto.builder()
                .employeeId(employeeId)
                .username(username)
                .phoneNumber(phoneNumber)
                .department(department)
                .departmentCode(departmentCode)
                .githubId(githubId)
                .build();

        // then
        assertThat(userResponseDto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(userResponseDto.getUsername()).isEqualTo(username);
        assertThat(userResponseDto.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(userResponseDto.getDepartment()).isEqualTo(department);
        assertThat(userResponseDto.getDepartmentCode()).isEqualTo(departmentCode);
        assertThat(userResponseDto.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("UserResponseDto 기본 생성자 테스트")
    void createUserResponseDtoWithNoArgsConstructor() {
        // when
        UserResponseDto userResponseDto = new UserResponseDto();

        // then
        assertThat(userResponseDto).isNotNull();
        assertThat(userResponseDto.getEmployeeId()).isNull();
        assertThat(userResponseDto.getUsername()).isNull();
        assertThat(userResponseDto.getPhoneNumber()).isNull();
        assertThat(userResponseDto.getDepartment()).isNull();
        assertThat(userResponseDto.getDepartmentCode()).isEqualTo(0);
        assertThat(userResponseDto.getGithubId()).isNull();
    }

    @Test
    @DisplayName("UserResponseDto 전체 생성자 테스트")
    void createUserResponseDtoWithAllArgsConstructor() {
        // given
        String employeeId = "CHN0001";
        String username = "김철수";
        String phoneNumber = "010-5678-1234";
        String department = "CORE_BANK";
        int departmentCode = 2;
        String githubId = "kim-cheolsu";

        // when
        UserResponseDto userResponseDto = new UserResponseDto(
                employeeId, username, phoneNumber, department, departmentCode, githubId
        );

        // then
        assertThat(userResponseDto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(userResponseDto.getUsername()).isEqualTo(username);
        assertThat(userResponseDto.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(userResponseDto.getDepartment()).isEqualTo(department);
        assertThat(userResponseDto.getDepartmentCode()).isEqualTo(departmentCode);
        assertThat(userResponseDto.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("UserResponseDto Setter 테스트")
    void testUserResponseDtoSetter() {
        // given
        UserResponseDto userResponseDto = new UserResponseDto();
        String employeeId = "CHN0001";
        String username = "이영희";
        String phoneNumber = "010-9999-8888";
        String department = "EXTERNAL";
        int departmentCode = 3;
        String githubId = "lee-younghee";

        // when
        userResponseDto.setEmployeeId(employeeId);
        userResponseDto.setUsername(username);
        userResponseDto.setPhoneNumber(phoneNumber);
        userResponseDto.setDepartment(department);
        userResponseDto.setDepartmentCode(departmentCode);
        userResponseDto.setGithubId(githubId);

        // then
        assertThat(userResponseDto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(userResponseDto.getUsername()).isEqualTo(username);
        assertThat(userResponseDto.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(userResponseDto.getDepartment()).isEqualTo(department);
        assertThat(userResponseDto.getDepartmentCode()).isEqualTo(departmentCode);
        assertThat(userResponseDto.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("UserResponseDto null 값 처리 테스트")
    void testUserResponseDtoWithNullValues() {
        // when
        UserResponseDto userResponseDto = UserResponseDto.builder()
                .employeeId(null)
                .username(null)
                .phoneNumber(null)
                .department(null)
                .departmentCode(0)
                .githubId(null)
                .build();

        // then
        assertThat(userResponseDto.getEmployeeId()).isNull();
        assertThat(userResponseDto.getUsername()).isNull();
        assertThat(userResponseDto.getPhoneNumber()).isNull();
        assertThat(userResponseDto.getDepartment()).isNull();
        assertThat(userResponseDto.getDepartmentCode()).isEqualTo(0);
        assertThat(userResponseDto.getGithubId()).isNull();
    }
}
