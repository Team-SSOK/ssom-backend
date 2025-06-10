package kr.ssok.ssom.backend.domain.user.dto;

import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class UserListResponseDtoTest {

    @Test
    @DisplayName("UserListResponseDto 생성 테스트")
    void createUserListResponseDto() {
        // given
        String id = "CHN0001";
        String username = "홍길동";
        String department = "CHANNEL";
        String githubId = "hong-gildong";

        // when
        UserListResponseDto dto = UserListResponseDto.builder()
                .id(id)
                .username(username)
                .department(department)
                .githubId(githubId)
                .build();

        // then
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getUsername()).isEqualTo(username);
        assertThat(dto.getDepartment()).isEqualTo(department);
        assertThat(dto.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("UserListResponseDto 기본 생성자 테스트")
    void createUserListResponseDtoWithNoArgsConstructor() {
        // when
        UserListResponseDto dto = new UserListResponseDto();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getUsername()).isNull();
        assertThat(dto.getDepartment()).isNull();
        assertThat(dto.getGithubId()).isNull();
    }

    @Test
    @DisplayName("UserListResponseDto 전체 생성자 테스트")
    void createUserListResponseDtoWithAllArgsConstructor() {
        // given
        String id = "CHN0001";
        String username = "김철수";
        String department = "CORE_BANK";
        String githubId = "kim-cheolsu";

        // when
        UserListResponseDto dto = new UserListResponseDto(id, username, department, githubId);

        // then
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getUsername()).isEqualTo(username);
        assertThat(dto.getDepartment()).isEqualTo(department);
        assertThat(dto.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("UserListResponseDto Setter 테스트")
    void testUserListResponseDtoSetter() {
        // given
        UserListResponseDto dto = new UserListResponseDto();
        String id = "CHN0001";
        String username = "이영희";
        String department = "EXTERNAL";
        String githubId = "lee-younghee";

        // when
        dto.setId(id);
        dto.setUsername(username);
        dto.setDepartment(department);
        dto.setGithubId(githubId);

        // then
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getUsername()).isEqualTo(username);
        assertThat(dto.getDepartment()).isEqualTo(department);
        assertThat(dto.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("UserListResponseDto from 메서드 - GitHub ID 있는 경우 테스트")
    void testFromMethodWithGithubId() {
        // given
        User user = User.builder()
                .id("CHN0001")
                .username("홍길동")
                .department(Department.CHANNEL)
                .githubId("hong-gildong")
                .build();

        // when
        UserListResponseDto dto = UserListResponseDto.from(user);

        // then
        assertThat(dto.getId()).isEqualTo(user.getId());
        assertThat(dto.getUsername()).isEqualTo(user.getUsername());
        assertThat(dto.getDepartment()).isEqualTo(user.getDepartment().name());
        assertThat(dto.getGithubId()).isEqualTo(user.getGithubId());
    }

    @Test
    @DisplayName("UserListResponseDto from 메서드 - GitHub ID 없는 경우 테스트")
    void testFromMethodWithoutGithubId() {
        // given
        User user = User.builder()
                .id("CHN0001")
                .username("김철수")
                .department(Department.CORE_BANK)
                .githubId(null)
                .build();

        // when
        UserListResponseDto dto = UserListResponseDto.from(user);

        // then
        assertThat(dto.getId()).isEqualTo(user.getId());
        assertThat(dto.getUsername()).isEqualTo(user.getUsername());
        assertThat(dto.getDepartment()).isEqualTo(user.getDepartment().name());
        assertThat(dto.getGithubId()).isNull();
    }

    @Test
    @DisplayName("UserListResponseDto from 메서드 - 빈 GitHub ID 테스트")
    void testFromMethodWithEmptyGithubId() {
        // given
        User user = User.builder()
                .id("CHN0001")
                .username("이영희")
                .department(Department.EXTERNAL)
                .githubId("")
                .build();

        // when
        UserListResponseDto dto = UserListResponseDto.from(user);

        // then
        assertThat(dto.getId()).isEqualTo(user.getId());
        assertThat(dto.getUsername()).isEqualTo(user.getUsername());
        assertThat(dto.getDepartment()).isEqualTo(user.getDepartment().name());
        assertThat(dto.getGithubId()).isEqualTo("");
    }

    @Test
    @DisplayName("UserListResponseDto 모든 부서 타입 테스트")
    void testAllDepartmentTypes() {
        // given & when & then
        Department[] departments = Department.values();
        
        for (Department dept : departments) {
            User user = User.builder()
                    .id("CHN0001")
                    .username("테스트사용자")
                    .department(dept)
                    .githubId("test-user")
                    .build();

            UserListResponseDto dto = UserListResponseDto.from(user);

            assertThat(dto.getDepartment()).isEqualTo(dept.name());
        }
    }

    @Test
    @DisplayName("UserListResponseDto null 값 처리 테스트")
    void testNullValueHandling() {
        // when
        UserListResponseDto dto = UserListResponseDto.builder()
                .id(null)
                .username(null)
                .department(null)
                .githubId(null)
                .build();

        // then
        assertThat(dto.getId()).isNull();
        assertThat(dto.getUsername()).isNull();
        assertThat(dto.getDepartment()).isNull();
        assertThat(dto.getGithubId()).isNull();
    }

    @Test
    @DisplayName("UserListResponseDto 다양한 부서별 사용자 테스트")
    void testUsersFromDifferentDepartments() {
        // given
        User channelUser = User.builder()
                .id("CHN0001")
                .username("채널개발자")
                .department(Department.CHANNEL)
                .githubId("channel-developer")
                .build();

        User coreBankUser = User.builder()
                .id("CHN0001")
                .username("계정계담당자")
                .department(Department.CORE_BANK)
                .githubId("core-bank-developer")
                .build();

        User externalUser = User.builder()
                .id("CHN0001")
                .username("대외계엔지니어")
                .department(Department.EXTERNAL)
                .githubId("external-engineer")
                .build();

        User operationUser = User.builder()
                .id("CHN0001")
                .username("운영계엔지니어")
                .department(Department.OPERATION)
                .githubId("operation-engineer")
                .build();

        // when
        UserListResponseDto channelDto = UserListResponseDto.from(channelUser);
        UserListResponseDto coreBankDto = UserListResponseDto.from(coreBankUser);
        UserListResponseDto externalDto = UserListResponseDto.from(externalUser);
        UserListResponseDto operationDto = UserListResponseDto.from(operationUser);

        // then
        assertThat(channelDto.getDepartment()).isEqualTo("CHANNEL");
        assertThat(coreBankDto.getDepartment()).isEqualTo("CORE_BANK");
        assertThat(externalDto.getDepartment()).isEqualTo("EXTERNAL");
        assertThat(operationDto.getDepartment()).isEqualTo("OPERATION");
    }

    @Test
    @DisplayName("UserListResponseDto 사원번호 형식 테스트")
    void testEmployeeIdFormats() {
        // given
        String[] employeeIds = {"CHN0001", "APP0123", "QA9999", "DESIGN001", "INFRA999"};

        for (String employeeId : employeeIds) {
            // when
            UserListResponseDto dto = UserListResponseDto.builder()
                    .id(employeeId)
                    .username("테스트사용자")
                    .department("CHANNEL")
                    .githubId("test-user")
                    .build();

            // then
            assertThat(dto.getId()).isEqualTo(employeeId);
        }
    }
}
