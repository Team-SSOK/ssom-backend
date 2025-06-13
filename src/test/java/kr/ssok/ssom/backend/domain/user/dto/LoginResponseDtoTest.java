package kr.ssok.ssom.backend.domain.user.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class LoginResponseDtoTest {

    @Test
    @DisplayName("LoginResponseDto 전체 필드 생성 테스트")
    void createLoginResponseDtoWithAllFields() {
        // given
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        String username = "홍길동";
        String department = "개발팀";
        Long expiresIn = 3600L;
        Boolean biometricEnabled = true;
        String lastLoginAt = "2024-06-10T10:30:00";

        // when
        LoginResponseDto loginResponseDto = LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(username)
                .department(department)
                .expiresIn(expiresIn)
                .biometricEnabled(biometricEnabled)
                .lastLoginAt(lastLoginAt)
                .build();

        // then
        assertThat(loginResponseDto.getAccessToken()).isEqualTo(accessToken);
        assertThat(loginResponseDto.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(loginResponseDto.getUsername()).isEqualTo(username);
        assertThat(loginResponseDto.getDepartment()).isEqualTo(department);
        assertThat(loginResponseDto.getExpiresIn()).isEqualTo(expiresIn);
        assertThat(loginResponseDto.getBiometricEnabled()).isEqualTo(biometricEnabled);
        assertThat(loginResponseDto.getLastLoginAt()).isEqualTo(lastLoginAt);
    }

    @Test
    @DisplayName("LoginResponseDto 기본 필드만 생성 테스트")
    void createLoginResponseDtoWithBasicFields() {
        // given
        String accessToken = "access.token.value";
        String refreshToken = "refresh.token.value";

        // when
        LoginResponseDto loginResponseDto = LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        // then
        assertThat(loginResponseDto.getAccessToken()).isEqualTo(accessToken);
        assertThat(loginResponseDto.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(loginResponseDto.getUsername()).isNull();
        assertThat(loginResponseDto.getDepartment()).isNull();
        assertThat(loginResponseDto.getExpiresIn()).isNull();
        assertThat(loginResponseDto.getBiometricEnabled()).isNull();
        assertThat(loginResponseDto.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("LoginResponseDto 기본 생성자 테스트")
    void createLoginResponseDtoWithNoArgsConstructor() {
        // when
        LoginResponseDto loginResponseDto = new LoginResponseDto();

        // then
        assertThat(loginResponseDto).isNotNull();
        assertThat(loginResponseDto.getAccessToken()).isNull();
        assertThat(loginResponseDto.getRefreshToken()).isNull();
        assertThat(loginResponseDto.getUsername()).isNull();
        assertThat(loginResponseDto.getDepartment()).isNull();
        assertThat(loginResponseDto.getExpiresIn()).isNull();
        assertThat(loginResponseDto.getBiometricEnabled()).isNull();
        assertThat(loginResponseDto.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("LoginResponseDto 전체 생성자 테스트")
    void createLoginResponseDtoWithAllArgsConstructor() {
        // given
        String accessToken = "access.token";
        String refreshToken = "refresh.token";
        String username = "김철수";
        String department = "QA팀";
        Long expiresIn = 7200L;
        Boolean biometricEnabled = false;
        String lastLoginAt = "2024-06-10T11:00:00";

        // when
        LoginResponseDto loginResponseDto = new LoginResponseDto(
                accessToken, refreshToken, null, username, department, 
                expiresIn, biometricEnabled, lastLoginAt
        );

        // then
        assertThat(loginResponseDto.getAccessToken()).isEqualTo(accessToken);
        assertThat(loginResponseDto.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(loginResponseDto.getUsername()).isEqualTo(username);
        assertThat(loginResponseDto.getDepartment()).isEqualTo(department);
        assertThat(loginResponseDto.getExpiresIn()).isEqualTo(expiresIn);
        assertThat(loginResponseDto.getBiometricEnabled()).isEqualTo(biometricEnabled);
        assertThat(loginResponseDto.getLastLoginAt()).isEqualTo(lastLoginAt);
    }

    @Test
    @DisplayName("LoginResponseDto Setter 테스트")
    void testLoginResponseDtoSetter() {
        // given
        LoginResponseDto loginResponseDto = new LoginResponseDto();
        String accessToken = "new.access.token";
        String refreshToken = "new.refresh.token";
        String username = "이영희";
        String department = "디자인팀";
        Long expiresIn = 1800L;
        Boolean biometricEnabled = true;
        String lastLoginAt = "2024-06-10T12:30:00";

        // when
        loginResponseDto.setAccessToken(accessToken);
        loginResponseDto.setRefreshToken(refreshToken);
        loginResponseDto.setUsername(username);
        loginResponseDto.setDepartment(department);
        loginResponseDto.setExpiresIn(expiresIn);
        loginResponseDto.setBiometricEnabled(biometricEnabled);
        loginResponseDto.setLastLoginAt(lastLoginAt);

        // then
        assertThat(loginResponseDto.getAccessToken()).isEqualTo(accessToken);
        assertThat(loginResponseDto.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(loginResponseDto.getUsername()).isEqualTo(username);
        assertThat(loginResponseDto.getDepartment()).isEqualTo(department);
        assertThat(loginResponseDto.getExpiresIn()).isEqualTo(expiresIn);
        assertThat(loginResponseDto.getBiometricEnabled()).isEqualTo(biometricEnabled);
        assertThat(loginResponseDto.getLastLoginAt()).isEqualTo(lastLoginAt);
    }

    @Test
    @DisplayName("LoginResponseDto 생체인증 관련 필드 테스트")
    void testBiometricRelatedFields() {
        // when - 생체인증 활성화 상태
        LoginResponseDto enabledDto = LoginResponseDto.builder()
                .biometricEnabled(true)
                .lastLoginAt("2024-06-10T10:00:00")
                .build();

        // when - 생체인증 비활성화 상태
        LoginResponseDto disabledDto = LoginResponseDto.builder()
                .biometricEnabled(false)
                .lastLoginAt(null)
                .build();

        // then
        assertThat(enabledDto.getBiometricEnabled()).isTrue();
        assertThat(enabledDto.getLastLoginAt()).isNotNull();
        
        assertThat(disabledDto.getBiometricEnabled()).isFalse();
        assertThat(disabledDto.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("LoginResponseDto 토큰 만료 시간 테스트")
    void testTokenExpirationTime() {
        // given
        Long shortExpiration = 900L; // 15분
        Long longExpiration = 86400L; // 24시간

        // when
        LoginResponseDto shortExpiryDto = LoginResponseDto.builder()
                .expiresIn(shortExpiration)
                .build();

        LoginResponseDto longExpiryDto = LoginResponseDto.builder()
                .expiresIn(longExpiration)
                .build();

        // then
        assertThat(shortExpiryDto.getExpiresIn()).isEqualTo(shortExpiration);
        assertThat(longExpiryDto.getExpiresIn()).isEqualTo(longExpiration);
        assertThat(longExpiryDto.getExpiresIn()).isGreaterThan(shortExpiryDto.getExpiresIn());
    }
}
