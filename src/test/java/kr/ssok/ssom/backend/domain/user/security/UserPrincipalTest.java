package kr.ssok.ssom.backend.domain.user.security;

import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserPrincipal 테스트")
class UserPrincipalTest {

    @Nested
    @DisplayName("UserPrincipal 생성 테스트")
    class UserPrincipalCreationTest {

        @Test
        @DisplayName("User 엔티티에서 UserPrincipal을 정상적으로 생성한다")
        void from_ValidUser_ReturnsUserPrincipal() {
            // given
            User user = User.builder()
                    .id("CHN0001")
                    .username("홍길동")
                    .password("encodedPassword")
                    .phoneNumber("010-1234-5678")
                    .department(Department.CHANNEL)
                    .githubId("hong123")
                    .build();

            // when
            UserPrincipal userPrincipal = UserPrincipal.from(user);

            // then
            assertThat(userPrincipal.getEmployeeId()).isEqualTo("CHN0001");
            assertThat(userPrincipal.getUsername()).isEqualTo("홍길동");
            assertThat(userPrincipal.getPhoneNumber()).isEqualTo("010-1234-5678");
            assertThat(userPrincipal.getDepartment()).isEqualTo(Department.CHANNEL);
            assertThat(userPrincipal.getGithubId()).isEqualTo("hong123");
        }

        @Test
        @DisplayName("GitHub ID가 null인 User에서도 정상적으로 생성된다")
        void from_UserWithNullGithubId_ReturnsUserPrincipal() {
            // given
            User user = User.builder()
                    .id("CORE0002")
                    .username("김철수")
                    .password("encodedPassword")
                    .phoneNumber("010-9876-5432")
                    .department(Department.CORE_BANK)
                    .githubId(null)
                    .build();

            // when
            UserPrincipal userPrincipal = UserPrincipal.from(user);

            // then
            assertThat(userPrincipal.getEmployeeId()).isEqualTo("CORE0002");
            assertThat(userPrincipal.getUsername()).isEqualTo("김철수");
            assertThat(userPrincipal.getPhoneNumber()).isEqualTo("010-9876-5432");
            assertThat(userPrincipal.getDepartment()).isEqualTo(Department.CORE_BANK);
            assertThat(userPrincipal.getGithubId()).isNull();
        }

        @Test
        @DisplayName("모든 부서 타입에 대해 정상적으로 생성된다")
        void from_AllDepartmentTypes_ReturnsUserPrincipal() {
            // given
            User[] users = {
                    createUserWithDepartment("CHN0001", Department.CHANNEL),
                    createUserWithDepartment("CORE0002", Department.CORE_BANK),
                    createUserWithDepartment("EXT0003", Department.EXTERNAL),
                    createUserWithDepartment("OPR0004", Department.OPERATION)
            };

            // when & then
            for (User user : users) {
                UserPrincipal userPrincipal = UserPrincipal.from(user);

                assertThat(userPrincipal.getEmployeeId()).isEqualTo(user.getId());
                assertThat(userPrincipal.getDepartment()).isEqualTo(user.getDepartment());
                assertThat(userPrincipal.getDepartmentCode()).isEqualTo(user.getDepartment().getCode());
                assertThat(userPrincipal.getDepartmentName()).isEqualTo(user.getDepartment().name());
            }
        }

        private User createUserWithDepartment(String employeeId, Department department) {
            return User.builder()
                    .id(employeeId)
                    .username("테스트사용자")
                    .password("password")
                    .phoneNumber("010-1234-5678")
                    .department(department)
                    .githubId("testuser")
                    .build();
        }
    }

    @Nested
    @DisplayName("부서 정보 조회 테스트")
    class DepartmentInfoTest {

        @Test
        @DisplayName("부서 코드를 정확히 반환한다")
        void getDepartmentCode_ReturnsCorrectCode() {
            // given
            User user = User.builder()
                    .id("CHN0001")
                    .username("홍길동")
                    .password("password")
                    .phoneNumber("010-1234-5678")
                    .department(Department.CHANNEL)
                    .githubId("hong123")
                    .build();

            UserPrincipal userPrincipal = UserPrincipal.from(user);

            // when
            int departmentCode = userPrincipal.getDepartmentCode();

            // then
            assertThat(departmentCode).isEqualTo(1);
        }

        @Test
        @DisplayName("부서명을 정확히 반환한다")
        void getDepartmentName_ReturnsCorrectName() {
            // given
            User user = User.builder()
                    .id("CORE0002")
                    .username("김철수")
                    .password("password")
                    .phoneNumber("010-9876-5432")
                    .department(Department.CORE_BANK)
                    .githubId("kim123")
                    .build();

            UserPrincipal userPrincipal = UserPrincipal.from(user);

            // when
            String departmentName = userPrincipal.getDepartmentName();

            // then
            assertThat(departmentName).isEqualTo("CORE_BANK");
        }

        @Test
        @DisplayName("모든 부서에 대해 코드와 이름이 일치한다")
        void departmentCodeAndName_ConsistentForAllDepartments() {
            // given
            Department[] departments = Department.values();

            for (Department department : departments) {
                User user = User.builder()
                        .id("TEST001")
                        .username("테스트")
                        .password("password")
                        .phoneNumber("010-1234-5678")
                        .department(department)
                        .githubId("test")
                        .build();

                UserPrincipal userPrincipal = UserPrincipal.from(user);

                // when & then
                assertThat(userPrincipal.getDepartmentCode()).isEqualTo(department.getCode());
                assertThat(userPrincipal.getDepartmentName()).isEqualTo(department.name());
                assertThat(userPrincipal.getDepartment()).isEqualTo(department);
            }
        }
    }

    @Nested
    @DisplayName("필드 접근 테스트")
    class FieldAccessTest {

        @Test
        @DisplayName("모든 필드에 정상적으로 접근할 수 있다")
        void allFields_AccessibleCorrectly() {
            // given
            String expectedEmployeeId = "EXT0003";
            String expectedUsername = "이영희";
            String expectedPhoneNumber = "010-5555-6666";
            Department expectedDepartment = Department.EXTERNAL;
            String expectedGithubId = "lee456";

            User user = User.builder()
                    .id(expectedEmployeeId)
                    .username(expectedUsername)
                    .password("password")
                    .phoneNumber(expectedPhoneNumber)
                    .department(expectedDepartment)
                    .githubId(expectedGithubId)
                    .build();

            UserPrincipal userPrincipal = UserPrincipal.from(user);

            // when & then
            assertThat(userPrincipal.getEmployeeId()).isEqualTo(expectedEmployeeId);
            assertThat(userPrincipal.getUsername()).isEqualTo(expectedUsername);
            assertThat(userPrincipal.getPhoneNumber()).isEqualTo(expectedPhoneNumber);
            assertThat(userPrincipal.getDepartment()).isEqualTo(expectedDepartment);
            assertThat(userPrincipal.getGithubId()).isEqualTo(expectedGithubId);
        }

        @Test
        @DisplayName("UserPrincipal은 immutable하다")
        void userPrincipal_IsImmutable() {
            // given
            User user = User.builder()
                    .id("OPR0004")
                    .username("박민수")
                    .password("password")
                    .phoneNumber("010-7777-8888")
                    .department(Department.OPERATION)
                    .githubId("park789")
                    .build();

            UserPrincipal userPrincipal = UserPrincipal.from(user);

            // when
            String originalEmployeeId = userPrincipal.getEmployeeId();
            String originalUsername = userPrincipal.getUsername();
            Department originalDepartment = userPrincipal.getDepartment();

            // User 객체 수정
            user.setUsername("변경된이름");
            user.setDepartment(Department.CHANNEL);

            // then - UserPrincipal은 변경되지 않음
            assertThat(userPrincipal.getEmployeeId()).isEqualTo(originalEmployeeId);
            assertThat(userPrincipal.getUsername()).isEqualTo(originalUsername);
            assertThat(userPrincipal.getDepartment()).isEqualTo(originalDepartment);
        }
    }
}
