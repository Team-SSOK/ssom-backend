package kr.ssok.ssom.backend.domain.user.service;

import kr.ssok.ssom.backend.domain.user.dto.*;
import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.repository.BiometricInfoRepository;
import kr.ssok.ssom.backend.domain.user.repository.UserRepository;
import kr.ssok.ssom.backend.domain.user.security.jwt.JwtTokenProvider;
import kr.ssok.ssom.backend.domain.user.service.Impl.UserServiceImpl;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BiometricInfoRepository biometricInfoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BiometricFailureService biometricFailureService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private SignupRequestDto signupRequest;
    private LoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
        // 테스트용 User 객체 생성
        testUser = User.builder()
                .id("CHN0001")
                .username("테스트사용자")
                .password("encodedPassword")
                .phoneNumber("010-1234-5678")
                .department(Department.CHANNEL)
                .githubId("test-user")
                .build();

        // 테스트용 SignupRequestDto 생성
        signupRequest = SignupRequestDto.builder()
                .username("신규사용자")
                .password("password123")
                .phoneNumber("010-9876-5432")
                .departmentCode(1) // CHANNEL
                .githubId("new-user")
                .build();

        // 테스트용 LoginRequestDto 생성
        loginRequest = LoginRequestDto.builder()
                .employeeId("CHN0001")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("회원가입 - 정상적으로 사용자가 등록된다")
    void registerUser_Success() {
        // Given
        given(userRepository.existsByPhoneNumber(signupRequest.getPhoneNumber())).willReturn(false);
        given(userRepository.findLastEmployeeIdByPrefix("CHN%")).willReturn(null);
        given(passwordEncoder.encode(signupRequest.getPassword())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(testUser);

        // When
        assertThatNoException().isThrownBy(() -> userService.registerUser(signupRequest));

        // Then
        then(userRepository).should().existsByPhoneNumber(signupRequest.getPhoneNumber());
        then(userRepository).should().findLastEmployeeIdByPrefix("CHN%");
        then(passwordEncoder).should().encode(signupRequest.getPassword());
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 - 중복된 전화번호로 가입 시 예외 발생")
    void registerUser_DuplicatePhoneNumber_ThrowsException() {
        // Given
        given(userRepository.existsByPhoneNumber(signupRequest.getPhoneNumber())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(signupRequest))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.USER_ALREADY_EXISTS);

        then(userRepository).should().existsByPhoneNumber(signupRequest.getPhoneNumber());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 - 연속된 사원번호가 생성된다")
    void registerUser_GeneratesSequentialEmployeeId() {
        // Given
        given(userRepository.existsByPhoneNumber(signupRequest.getPhoneNumber())).willReturn(false);
        given(userRepository.findLastEmployeeIdByPrefix("CHN%")).willReturn("CHN0005");
        given(passwordEncoder.encode(signupRequest.getPassword())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(testUser);

        // When
        userService.registerUser(signupRequest);

        // Then
        then(userRepository).should().save(argThat(user -> 
            user.getId().equals("CHN0006") &&
            user.getDepartment().equals(Department.CHANNEL)
        ));
    }

    @Test
    @DisplayName("로그인 - 정상적으로 로그인된다")
    void login_Success() {
        // Given
        given(userRepository.findByEmployeeId(loginRequest.getEmployeeId())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).willReturn(true);
        given(jwtTokenProvider.createAccessToken(testUser.getId())).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(testUser.getId())).willReturn("refreshToken");
        given(jwtTokenProvider.getTokenExpirationTime("refreshToken")).willReturn(3600L);
        given(biometricInfoRepository.existsByEmployeeIdAndIsActiveTrue(testUser.getId())).willReturn(true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When
        LoginResponseDto response = userService.login(loginRequest);

        // Then
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(response.getDepartment()).isEqualTo(testUser.getDepartment().getPrefix());
        assertThat(response.getBiometricEnabled()).isTrue();
        assertThat(response.getExpiresIn()).isEqualTo(3600L);

        then(valueOperations).should().set(eq("refresh:token:CHN0001"), eq("refreshToken"), eq(3600L), any(TimeUnit.class));
        then(biometricFailureService).should().unblockAllDevicesForUser(testUser.getId());
    }

    @Test
    @DisplayName("로그인 - 존재하지 않는 사용자로 로그인 시 예외 발생")
    void login_UserNotFound_ThrowsException() {
        // Given
        given(userRepository.findByEmployeeId(loginRequest.getEmployeeId())).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.USER_NOT_FOUND);

        then(passwordEncoder).should(never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 - 잘못된 비밀번호로 로그인 시 예외 발생")
    void login_InvalidPassword_ThrowsException() {
        // Given
        given(userRepository.findByEmployeeId(loginRequest.getEmployeeId())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_PASSWORD);

        then(jwtTokenProvider).should(never()).createAccessToken(anyString());
    }

    @Test
    @DisplayName("토큰 갱신 - 정상적으로 토큰이 갱신된다")
    void refreshToken_Success() {
        // Given
        String refreshToken = "validRefreshToken";
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(testUser.getId());
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:token:CHN0001")).willReturn(refreshToken);
        given(userRepository.findByEmployeeId(testUser.getId())).willReturn(Optional.of(testUser));
        given(jwtTokenProvider.createAccessToken(testUser.getId())).willReturn("newAccessToken");
        given(jwtTokenProvider.createRefreshToken(testUser.getId())).willReturn("newRefreshToken");
        given(jwtTokenProvider.getTokenExpirationTime("newRefreshToken")).willReturn(3600L);
        given(biometricInfoRepository.existsByEmployeeIdAndIsActiveTrue(testUser.getId())).willReturn(false);

        // When
        LoginResponseDto response = userService.refreshToken(refreshToken);

        // Then
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(response.getBiometricEnabled()).isFalse();

        then(redisTemplate).should().delete("refresh:token:CHN0001");
        then(valueOperations).should().set(eq("refresh:token:CHN0001"), eq("newRefreshToken"), eq(3600L), any(TimeUnit.class));
    }

    @Test
    @DisplayName("토큰 갱신 - 유효하지 않은 토큰으로 갱신 시 예외 발생")
    void refreshToken_InvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalidToken";
        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.refreshToken(invalidToken))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_REFRESH_TOKEN);

        then(jwtTokenProvider).should().validateToken(invalidToken);
    }

    @Test
    @DisplayName("토큰 갱신 - Redis에 저장된 토큰과 불일치 시 예외 발생")
    void refreshToken_TokenMismatch_ThrowsException() {
        // Given
        String refreshToken = "validRefreshToken";
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(testUser.getId());
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:token:CHN0001")).willReturn("differentToken");

        // When & Then
        assertThatThrownBy(() -> userService.refreshToken(refreshToken))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_REFRESH_TOKEN);

        then(userRepository).should(never()).findByEmployeeId(anyString());
    }

    @Test
    @DisplayName("로그아웃 - 정상적으로 로그아웃된다")
    void logout_Success() {
        // Given
        String accessToken = "Bearer validAccessToken";
        String token = "validAccessToken";
        given(jwtTokenProvider.resolveToken(accessToken)).willReturn(token);
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(testUser.getId());
        given(jwtTokenProvider.getTokenExpirationTime(token)).willReturn(3600L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When
        assertThatNoException().isThrownBy(() -> userService.logout(accessToken));

        // Then
        then(redisTemplate).should().delete("refresh:token:CHN0001");
        then(valueOperations).should().set(eq("blacklist:token:validAccessToken"), eq("logout"), eq(3600L), any(TimeUnit.class));
    }

    @Test
    @DisplayName("로그아웃 - 유효하지 않은 토큰으로 로그아웃 시 예외 발생")
    void logout_InvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalidToken";
        given(jwtTokenProvider.resolveToken(invalidToken)).willReturn(null);

        // When & Then
        assertThatThrownBy(() -> userService.logout(invalidToken))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_TOKEN);

        then(redisTemplate).should(never()).delete(anyString());
    }

    @Test
    @DisplayName("사용자 정보 조회 - 정상적으로 사용자 정보를 반환한다")
    void getUserInfo_Success() {
        // Given
        given(userRepository.findByEmployeeId(testUser.getId())).willReturn(Optional.of(testUser));

        // When
        UserResponseDto response = userService.getUserInfo(testUser.getId());

        // Then
        assertThat(response.getEmployeeId()).isEqualTo(testUser.getId());
        assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(response.getPhoneNumber()).isEqualTo(testUser.getPhoneNumber());
        assertThat(response.getDepartment()).isEqualTo(testUser.getDepartment().name());
        assertThat(response.getDepartmentCode()).isEqualTo(testUser.getDepartment().getCode());
        assertThat(response.getGithubId()).isEqualTo(testUser.getGithubId());
    }

    @Test
    @DisplayName("사원번호로 사용자 조회 - 정상적으로 사용자를 반환한다")
    void findUserByEmployeeId_Success() {
        // Given
        given(userRepository.findByEmployeeId(testUser.getId())).willReturn(Optional.of(testUser));

        // When
        User foundUser = userService.findUserByEmployeeId(testUser.getId());

        // Then
        assertThat(foundUser).isEqualTo(testUser);
    }

    @Test
    @DisplayName("사원번호로 사용자 조회 - 존재하지 않는 사용자 조회 시 예외 발생")
    void findUserByEmployeeId_UserNotFound_ThrowsException() {
        // Given
        String nonExistentId = "NONE0001";
        given(userRepository.findByEmployeeId(nonExistentId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findUserByEmployeeId(nonExistentId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호 변경 - 정상적으로 비밀번호가 변경된다")
    void changePassword_Success() {
        // Given
        PasswordChangeRequestDto request = PasswordChangeRequestDto.builder()
                .currentPassword("currentPassword")
                .newPassword("newPassword")
                .confirmPassword("newPassword")
                .build();

        given(userRepository.findByEmployeeId(testUser.getId())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPassword())).willReturn(true);
        given(passwordEncoder.matches(request.getNewPassword(), testUser.getPassword())).willReturn(false);
        given(passwordEncoder.encode(request.getNewPassword())).willReturn("newEncodedPassword");
        given(userRepository.save(testUser)).willReturn(testUser);

        // When
        assertThatNoException().isThrownBy(() -> userService.changePassword(testUser.getId(), request));

        // Then
        then(userRepository).should().save(testUser);
    }

    @Test
    @DisplayName("비밀번호 변경 - 새 비밀번호와 확인 비밀번호 불일치 시 예외 발생")
    void changePassword_PasswordMismatch_ThrowsException() {
        // Given
        PasswordChangeRequestDto request = PasswordChangeRequestDto.builder()
                .currentPassword("currentPassword")
                .newPassword("newPassword")
                .confirmPassword("differentPassword")
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(testUser.getId(), request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.PASSWORD_CONFIRM_MISMATCH);

        then(userRepository).should(never()).findByEmployeeId(anyString());
    }

    @Test
    @DisplayName("비밀번호 변경 - 현재 비밀번호 불일치 시 예외 발생")
    void changePassword_InvalidCurrentPassword_ThrowsException() {
        // Given
        PasswordChangeRequestDto request = PasswordChangeRequestDto.builder()
                .currentPassword("wrongPassword")
                .newPassword("newPassword")
                .confirmPassword("newPassword")
                .build();

        given(userRepository.findByEmployeeId(testUser.getId())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPassword())).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(testUser.getId(), request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_CURRENT_PASSWORD);

        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("비밀번호 변경 - 새 비밀번호가 현재 비밀번호와 같을 시 예외 발생")
    void changePassword_SameAsCurrentPassword_ThrowsException() {
        // Given
        PasswordChangeRequestDto request = PasswordChangeRequestDto.builder()
                .currentPassword("currentPassword")
                .newPassword("samePassword")
                .confirmPassword("samePassword")
                .build();

        given(userRepository.findByEmployeeId(testUser.getId())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPassword())).willReturn(true);
        given(passwordEncoder.matches(request.getNewPassword(), testUser.getPassword())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(testUser.getId(), request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.SAME_AS_CURRENT_PASSWORD);

        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("모든 사용자 목록 조회 - 정상적으로 사용자 목록을 반환한다")
    void getAllUsers_Success() {
        // Given
        User user1 = User.builder()
                .id("CHN0001")
                .username("사용자1")
                .department(Department.CHANNEL)
                .build();

        User user2 = User.builder()
                .id("CORE0001")
                .username("사용자2")
                .department(Department.CORE_BANK)
                .build();

        List<User> users = List.of(user1, user2);
        given(userRepository.findAllByOrderByIdAsc()).willReturn(users);

        // When
        List<UserListResponseDto> response = userService.getAllUsers();

        // Then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getId()).isEqualTo("CHN0001");
        assertThat(response.get(0).getUsername()).isEqualTo("사용자1");
        assertThat(response.get(1).getId()).isEqualTo("CORE0001");
        assertThat(response.get(1).getUsername()).isEqualTo("사용자2");
    }

    @Test
    @DisplayName("모든 사용자 목록 조회 - 사용자가 없는 경우 빈 목록을 반환한다")
    void getAllUsers_EmptyList() {
        // Given
        given(userRepository.findAllByOrderByIdAsc()).willReturn(List.of());

        // When
        List<UserListResponseDto> response = userService.getAllUsers();

        // Then
        assertThat(response).isEmpty();
    }
}
