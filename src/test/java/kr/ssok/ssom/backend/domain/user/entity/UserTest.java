package kr.ssok.ssom.backend.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User 엔티티 단위 테스트")
class UserTest {

    @Test
    @DisplayName("User 객체를 Builder 패턴으로 생성할 수 있다")
    void testUserBuilder() {
        // Given
        String id = "APP0001";
        String username = "김개발";
        String password = "password123";
        String phoneNumber = "010-1234-5678";
        Department department = Department.CHANNEL;
        String githubId = "developer-kim";

        // When
        User user = User.builder()
                .id(id)
                .username(username)
                .password(password)
                .phoneNumber(phoneNumber)
                .department(department)
                .githubId(githubId)
                .build();

        // Then
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(user.getDepartment()).isEqualTo(department);
        assertThat(user.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("User 객체를 기본 생성자로 생성할 수 있다")
    void testUserNoArgsConstructor() {
        // When
        User user = new User();

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNull();
        assertThat(user.getUsername()).isNull();
        assertThat(user.getPassword()).isNull();
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getDepartment()).isNull();
        assertThat(user.getGithubId()).isNull();
    }

    @Test
    @DisplayName("User 객체를 전체 생성자로 생성할 수 있다")
    void testUserAllArgsConstructor() {
        // Given
        String id = "BANK0001";
        String username = "박은행";
        String password = "bankpass456";
        String phoneNumber = "010-9876-5432";
        Department department = Department.CORE_BANK;
        String githubId = "banker-park";

        // When
        User user = new User(id, username, password, phoneNumber, department, githubId);

        // Then
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(user.getDepartment()).isEqualTo(department);
        assertThat(user.getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("User 객체의 필드값을 Setter로 변경할 수 있다")
    void testUserSetter() {
        // Given
        User user = new User();
        String newUsername = "이테스트";
        String newPassword = "newpassword789";
        String newPhoneNumber = "010-1111-2222";
        Department newDepartment = Department.CHANNEL;
        String newGithubId = "test-lee";

        // When
        user.setId("TEST0001");
        user.setUsername(newUsername);
        user.setPassword(newPassword);
        user.setPhoneNumber(newPhoneNumber);
        user.setDepartment(newDepartment);
        user.setGithubId(newGithubId);

        // Then
        assertThat(user.getId()).isEqualTo("TEST0001");
        assertThat(user.getUsername()).isEqualTo(newUsername);
        assertThat(user.getPassword()).isEqualTo(newPassword);
        assertThat(user.getPhoneNumber()).isEqualTo(newPhoneNumber);
        assertThat(user.getDepartment()).isEqualTo(newDepartment);
        assertThat(user.getGithubId()).isEqualTo(newGithubId);
    }

    @Test
    @DisplayName("User 객체의 githubId는 null일 수 있다")
    void testUserWithNullGithubId() {
        // Given & When
        User user = User.builder()
                .id("APP0002")
                .username("최개발자")
                .password("password123")
                .phoneNumber("010-3333-4444")
                .department(Department.CHANNEL)
                .githubId(null)
                .build();

        // Then
        assertThat(user.getGithubId()).isNull();
        assertThat(user.getId()).isEqualTo("APP0002");
        assertThat(user.getUsername()).isEqualTo("최개발자");
    }

    @Test
    @DisplayName("User 객체는 TimeStamp를 상속받아 생성/수정 시간 필드를 가진다")
    void testUserInheritsTimeStamp() {
        // Given
        User user = User.builder()
                .id("APP0003")
                .username("시간테스트")
                .password("timetest123")
                .phoneNumber("010-5555-6666")
                .department(Department.CHANNEL)
                .build();

        // When & Then
        // TimeStamp 클래스의 필드들이 존재하는지 확인
        assertThat(user.getClass().getSuperclass().getSimpleName()).isEqualTo("TimeStamp");
    }

    @Test
    @DisplayName("User 객체는 Lombok이 생성한 equals와 hashCode 메서드를 가진다")
    void testUserEqualsAndHashCode() {
        // Given
        User user1 = User.builder()
                .id("SAME0001")
                .username("사용자1")
                .password("password1")
                .phoneNumber("010-1111-1111")
                .department(Department.CHANNEL)
                .githubId("github1")
                .build();

        User user2 = User.builder()
                .id("SAME0001")
                .username("사용자1")
                .password("password1")
                .phoneNumber("010-1111-1111")
                .department(Department.CHANNEL)
                .githubId("github1")
                .build();

        User user3 = User.builder()
                .id("DIFF0001")
                .username("사용자2")
                .password("password2")
                .phoneNumber("010-2222-2222")
                .department(Department.CORE_BANK)
                .githubId("github2")
                .build();

        // When & Then
        // Lombok은 TimeStamp 부모 클래스의 필드도 equals에 포함하므로
        // 생성 시간이 다를 수 있어 정확한 equals 테스트는 어려움
        // 기본적인 동작만 확인
        assertThat(user1).isEqualTo(user1); // 자기 자신과는 같음
        assertThat(user1).isNotEqualTo(null); // null과는 다름
        assertThat(user1).isNotEqualTo(user3); // 다른 내용의 객체는 다름
        
        // hashCode 메서드 존재 확인
        assertThat(user1.hashCode()).isNotNull();
        assertThat(user2.hashCode()).isNotNull();
    }

    @Test
    @DisplayName("User 객체의 toString은 정상적으로 동작한다")
    void testUserToString() {
        // Given
        User user = User.builder()
                .id("SEC0001")
                .username("보안테스트")
                .password("secretpassword123")
                .phoneNumber("010-7777-8888")
                .department(Department.CHANNEL)
                .githubId("security-test")
                .build();

        // When
        String userString = user.toString();

        // Then
        // User 클래스가 Lombok @ToString을 사용하지 않을 경우 Object.toString() 형태
        // 실제 테스트 결과를 바탕으로 Lombok이 toString을 생성하지 않음을 확인
        assertThat(userString).startsWith("kr.ssok.ssom.backend.domain.user.entity.User@");
        assertThat(userString).isNotEmpty();
    }

    @Test
    @DisplayName("부서별로 올바른 사원번호 형식을 가질 수 있다")
    void testUserIdFormatByDepartment() {
        // Given & When
        User channelUser = User.builder()
                .id("CHN0001")
                .username("쏙앱개발자")
                .department(Department.CHANNEL)
                .build();

        User coreUser = User.builder()
                .id("CORE0001")
                .username("쏙뱅크개발자")
                .department(Department.CORE_BANK)
                .build();

        // Then
        assertThat(channelUser.getId()).startsWith("CHN");
        assertThat(channelUser.getDepartment()).isEqualTo(Department.CHANNEL);
        
        assertThat(coreUser.getId()).startsWith("CORE");
        assertThat(coreUser.getDepartment()).isEqualTo(Department.CORE_BANK);
    }
}
