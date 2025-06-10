package kr.ssok.ssom.backend.domain.user.dto;

import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class UserSearchResponseDtoTest {

    @Test
    @DisplayName("UserSearchResponseDto 생성 테스트")
    void createUserSearchResponseDto() {
        // given
        String employeeId = "CHN0001";
        String username = "홍길동";
        Department department = Department.CHANNEL;
        Boolean hasGithubId = true;
        String githubId = "hong-gildong";

        // when
        UserSearchResponseDto dto = UserSearchResponseDto.builder()
                .employeeId(employeeId)
                .username(username)
                .department(department)
                .hasGithubId(hasGithubId)
                .githubId(githubId)
                .build();

        // then
        assertThat(dto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(dto.getUsername()).isEqualTo(username);
        assertThat(dto.getDepartment()).isEqualTo(department);
        assertThat(dto.getHasGithubId()).isEqualTo(hasGithubId);
        assertThat(dto.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("UserSearchResponseDto 기본 생성자 테스트")
    void createUserSearchResponseDtoWithNoArgsConstructor() {
        // when
        UserSearchResponseDto dto = new UserSearchResponseDto();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getEmployeeId()).isNull();
        assertThat(dto.getUsername()).isNull();
        assertThat(dto.getDepartment()).isNull();
        assertThat(dto.getHasGithubId()).isNull();
        assertThat(dto.getGithubId()).isNull();
    }

    @Test
    @DisplayName("UserSearchResponseDto 전체 생성자 테스트")
    void createUserSearchResponseDtoWithAllArgsConstructor() {
        // given
        String employeeId = "CHN0001";
        String username = "김철수";
        Department department = Department.CORE_BANK;
        Boolean hasGithubId = false;
        String githubId = null;

        // when
        UserSearchResponseDto dto = new UserSearchResponseDto(
                employeeId, username, department, hasGithubId, githubId
        );

        // then
        assertThat(dto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(dto.getUsername()).isEqualTo(username);
        assertThat(dto.getDepartment()).isEqualTo(department);
        assertThat(dto.getHasGithubId()).isEqualTo(hasGithubId);
        assertThat(dto.getGithubId()).isNull();
    }

    @Test
    @DisplayName("UserSearchResponseDto Setter 테스트")
    void testUserSearchResponseDtoSetter() {
        // given
        UserSearchResponseDto dto = new UserSearchResponseDto();
        String employeeId = "CHN0001";
        String username = "이영희";
        Department department = Department.EXTERNAL;
        Boolean hasGithubId = true;
        String githubId = "lee-younghee";

        // when
        dto.setEmployeeId(employeeId);
        dto.setUsername(username);
        dto.setDepartment(department);
        dto.setHasGithubId(hasGithubId);
        dto.setGithubId(githubId);

        // then
        assertThat(dto.getEmployeeId()).isEqualTo(employeeId);
        assertThat(dto.getUsername()).isEqualTo(username);
        assertThat(dto.getDepartment()).isEqualTo(department);
        assertThat(dto.getHasGithubId()).isEqualTo(hasGithubId);
        assertThat(dto.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("UserSearchResponseDto from 메서드 - GitHub ID 있는 경우 테스트")
    void testFromMethodWithGithubId() {
        // given
        User user = User.builder()
                .id("CHN0001")
                .username("홍길동")
                .department(Department.CHANNEL)
                .githubId("hong-gildong")
                .build();

        // when
        UserSearchResponseDto dto = UserSearchResponseDto.from(user);

        // then
        assertThat(dto.getEmployeeId()).isEqualTo(user.getId());
        assertThat(dto.getUsername()).isEqualTo(user.getUsername());
        assertThat(dto.getDepartment()).isEqualTo(user.getDepartment());
        assertThat(dto.getHasGithubId()).isTrue();
        assertThat(dto.getGithubId()).isEqualTo(user.getGithubId());
    }

    @Test
    @DisplayName("UserSearchResponseDto from 메서드 - GitHub ID 없는 경우 테스트")
    void testFromMethodWithoutGithubId() {
        // given
        User user = User.builder()
                .id("CHN0001")
                .username("김철수")
                .department(Department.CORE_BANK)
                .githubId(null)
                .build();

        // when
        UserSearchResponseDto dto = UserSearchResponseDto.from(user);

        // then
        assertThat(dto.getEmployeeId()).isEqualTo(user.getId());
        assertThat(dto.getUsername()).isEqualTo(user.getUsername());
        assertThat(dto.getDepartment()).isEqualTo(user.getDepartment());
        assertThat(dto.getHasGithubId()).isFalse();
        assertThat(dto.getGithubId()).isNull();
    }

    @Test
    @DisplayName("UserSearchResponseDto from 메서드 - GitHub ID 빈 문자열인 경우 테스트")
    void testFromMethodWithEmptyGithubId() {
        // given
        User user = User.builder()
                .id("CHN0001")
                .username("이영희")
                .department(Department.EXTERNAL)
                .githubId("")
                .build();

        // when
        UserSearchResponseDto dto = UserSearchResponseDto.from(user);

        // then
        assertThat(dto.getEmployeeId()).isEqualTo(user.getId());
        assertThat(dto.getUsername()).isEqualTo(user.getUsername());
        assertThat(dto.getDepartment()).isEqualTo(user.getDepartment());
        assertThat(dto.getHasGithubId()).isFalse();
        assertThat(dto.getGithubId()).isEqualTo("");
    }

    @Test
    @DisplayName("UserSearchResponseDto from 메서드 - GitHub ID 공백 문자열인 경우 테스트")
    void testFromMethodWithWhitespaceGithubId() {
        // given
        User user = User.builder()
                .id("CHN0001")
                .username("박민수")
                .department(Department.OPERATION)
                .githubId("   ")
                .build();

        // when
        UserSearchResponseDto dto = UserSearchResponseDto.from(user);

        // then
        assertThat(dto.getEmployeeId()).isEqualTo(user.getId());
        assertThat(dto.getUsername()).isEqualTo(user.getUsername());
        assertThat(dto.getDepartment()).isEqualTo(user.getDepartment());
        assertThat(dto.getHasGithubId()).isFalse();
        assertThat(dto.getGithubId()).isEqualTo("   ");
    }

    @Test
    @DisplayName("UserSearchResponseDto 모든 부서 타입 테스트")
    void testAllDepartmentTypes() {
        // given & when & then
        Department[] departments = Department.values();
        
        for (Department dept : departments) {
            UserSearchResponseDto dto = UserSearchResponseDto.builder()
                    .employeeId("CHN0001")
                    .username("테스트사용자")
                    .department(dept)
                    .hasGithubId(true)
                    .githubId("test-user")
                    .build();

            assertThat(dto.getDepartment()).isEqualTo(dept);
        }
    }

    @Test
    @DisplayName("UserSearchResponseDto hasGithubId Boolean 값 테스트")
    void testHasGithubIdBooleanValues() {
        // given & when
        UserSearchResponseDto trueDto = UserSearchResponseDto.builder()
                .hasGithubId(true)
                .build();

        UserSearchResponseDto falseDto = UserSearchResponseDto.builder()
                .hasGithubId(false)
                .build();

        UserSearchResponseDto nullDto = UserSearchResponseDto.builder()
                .hasGithubId(null)
                .build();

        // then
        assertThat(trueDto.getHasGithubId()).isTrue();
        assertThat(falseDto.getHasGithubId()).isFalse();
        assertThat(nullDto.getHasGithubId()).isNull();
    }
}
