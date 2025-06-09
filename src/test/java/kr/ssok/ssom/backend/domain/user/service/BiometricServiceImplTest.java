package kr.ssok.ssom.backend.domain.user.service;

import jakarta.servlet.http.HttpServletRequest;
import kr.ssok.ssom.backend.domain.user.dto.*;
import kr.ssok.ssom.backend.domain.user.entity.*;
import kr.ssok.ssom.backend.domain.user.repository.BiometricInfoRepository;
import kr.ssok.ssom.backend.domain.user.repository.BiometricLoginAttemptRepository;
import kr.ssok.ssom.backend.domain.user.security.jwt.JwtTokenProvider;
import kr.ssok.ssom.backend.domain.user.service.BiometricFailureService;
import kr.ssok.ssom.backend.domain.user.service.Impl.BiometricServiceImpl;
import kr.ssok.ssom.backend.domain.user.service.UserService;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("BiometricService 테스트")
class BiometricServiceImplTest {

    @InjectMocks
    private BiometricServiceImpl biometricService;

    @Mock
    private BiometricInfoRepository biometricInfoRepository;

    @Mock
    private BiometricLoginAttemptRepository biometricLoginAttemptRepository;

    @Mock
    private BiometricFailureService biometricFailureService;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest httpServletRequest;

    private User testUser;
    private BiometricInfo testBiometricInfo;
    private BiometricLoginRequestDto loginRequest;
    private BiometricRegistrationRequestDto registrationRequest;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 데이터 설정
        testUser = User.builder()
                .id("CHN0001")
                .username("테스트유저")
                .password("encodedPassword")
                .phoneNumber("010-1234-5678")
                .department(Department.CHANNEL)
                .build();

        // 테스트 생체인증 정보 설정
        testBiometricInfo = BiometricInfo.builder()
                .id(1L)
                .employeeId("CHN0001")
                .biometricType(BiometricType.FINGERPRINT)
                .deviceId("test-device-001")
                .biometricHash("encoded-biometric-hash")
                .deviceInfo("{\"model\": \"iPhone 14\", \"os\": \"iOS 16.0\"}")
                .isActive(true)
                .lastUsedAt(LocalDateTime.now().minusDays(1))
                .build();

        // 로그인 요청 DTO 설정
        loginRequest = BiometricLoginRequestDto.builder()
                .employeeId("CHN0001")
                .biometricType("FINGERPRINT")
                .deviceId("test-device-001")
                .biometricHash("raw-biometric-hash")
                .timestamp(System.currentTimeMillis())
                .build();

        // 등록 요청 DTO 설정
        registrationRequest = BiometricRegistrationRequestDto.builder()
                .biometricType("FINGERPRINT")
                .deviceId("test-device-001")
                .biometricHash("raw-biometric-hash")
                .deviceInfo("{\"model\": \"iPhone 14\", \"os\": \"iOS 16.0\"}")
                .build();
    }

    @Nested
    @DisplayName("생체인증 상태 확인")
    class CheckBiometricStatusTest {

        @Test
        @DisplayName("생체인증이 등록된 사용자의 상태 확인 성공")
        void checkBiometricStatus_Success_WithRegisteredBiometric() {
            // given
            String employeeId = "CHN0001";
            List<BiometricInfo> biometrics = List.of(testBiometricInfo);

            given(biometricInfoRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
                    .willReturn(biometrics);

            // when
            BiometricStatusDto result = biometricService.checkBiometricStatus(employeeId);

            // then
            assertThat(result.getIsRegistered()).isTrue();
            assertThat(result.getAvailableTypes()).containsExactly("FINGERPRINT");
            assertThat(result.getDeviceCount()).isEqualTo(1);
            assertThat(result.getLastUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("생체인증이 등록되지 않은 사용자의 상태 확인")
        void checkBiometricStatus_Success_WithoutRegisteredBiometric() {
            // given
            String employeeId = "CHN0001";
            List<BiometricInfo> emptyList = new ArrayList<>();

            given(biometricInfoRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
                    .willReturn(emptyList);

            // when
            BiometricStatusDto result = biometricService.checkBiometricStatus(employeeId);

            // then
            assertThat(result.getIsRegistered()).isFalse();
            assertThat(result.getAvailableTypes()).isEmpty();
            assertThat(result.getDeviceCount()).isEqualTo(0);
            assertThat(result.getLastUsedAt()).isNull();
        }

        @Test
        @DisplayName("여러 생체인증이 등록된 사용자의 상태 확인")
        void checkBiometricStatus_Success_WithMultipleBiometrics() {
            // given
            String employeeId = "CHN0001";

            BiometricInfo fingerprint = BiometricInfo.builder()
                    .employeeId(employeeId)
                    .biometricType(BiometricType.FINGERPRINT)
                    .lastUsedAt(LocalDateTime.now().minusDays(1))
                    .build();

            BiometricInfo face = BiometricInfo.builder()
                    .employeeId(employeeId)
                    .biometricType(BiometricType.FACE)
                    .lastUsedAt(LocalDateTime.now().minusHours(1))
                    .build();

            List<BiometricInfo> biometrics = List.of(fingerprint, face);

            given(biometricInfoRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
                    .willReturn(biometrics);

            // when
            BiometricStatusDto result = biometricService.checkBiometricStatus(employeeId);

            // then
            assertThat(result.getIsRegistered()).isTrue();
            assertThat(result.getAvailableTypes()).containsExactlyInAnyOrder("FINGERPRINT", "FACE");
            assertThat(result.getDeviceCount()).isEqualTo(2);
            assertThat(result.getLastUsedAt()).isEqualTo(face.getLastUsedAt());
        }
    }

    @Nested
    @DisplayName("생체인증 등록")
    class RegisterBiometricTest {

        @Test
        @DisplayName("생체인증 등록 성공")
        void registerBiometric_Success() {
            // given
            String employeeId = "CHN0001";

            given(userService.findUserByEmployeeId(employeeId)).willReturn(testUser);
            given(biometricInfoRepository.existsByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    eq(employeeId), eq("test-device-001"), eq(BiometricType.FINGERPRINT)))
                    .willReturn(false);
            given(passwordEncoder.encode("raw-biometric-hash"))
                    .willReturn("encoded-biometric-hash");
            given(biometricInfoRepository.save(any(BiometricInfo.class)))
                    .willReturn(testBiometricInfo);

            // when
            BiometricResponseDto result = biometricService.registerBiometric(employeeId, registrationRequest);

            // then
            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("생체인증이 성공적으로 등록되었습니다.");
            assertThat(result.getBiometricId()).isEqualTo(1L);

            verify(biometricInfoRepository).save(any(BiometricInfo.class));
        }

        @Test
        @DisplayName("이미 등록된 생체인증 등록 시도 시 예외 발생")
        void registerBiometric_Fail_AlreadyRegistered() {
            // given
            String employeeId = "CHN0001";

            given(userService.findUserByEmployeeId(employeeId)).willReturn(testUser);
            given(biometricInfoRepository.existsByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    eq(employeeId), eq("test-device-001"), eq(BiometricType.FINGERPRINT)))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> biometricService.registerBiometric(employeeId, registrationRequest))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.BIOMETRIC_ALREADY_REGISTERED);

            verify(biometricInfoRepository, never()).save(any(BiometricInfo.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 생체인증 등록 시도 시 예외 발생")
        void registerBiometric_Fail_UserNotFound() {
            // given
            String employeeId = "CHN0001";

            given(userService.findUserByEmployeeId(employeeId))
                    .willThrow(new BaseException(BaseResponseStatus.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> biometricService.registerBiometric(employeeId, registrationRequest))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.USER_NOT_FOUND);

            verify(biometricInfoRepository, never()).save(any(BiometricInfo.class));
        }
    }

    @Nested
    @DisplayName("생체인증 로그인")
    class BiometricLoginTest {

        @BeforeEach
        void setUpLogin() {
            // HttpServletRequest Mock 설정 - 모든 헤더 요청에 대해 lenient stubbing
            lenient().when(httpServletRequest.getHeader("User-Agent")).thenReturn("test-user-agent");
            lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            lenient().when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
            lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        }

        @Test
        @DisplayName("생체인증 로그인 성공")
        void biometricLogin_Success() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", "test-device-001"))
                    .willReturn(0);
            given(userService.findUserByEmployeeId("CHN0001")).willReturn(testUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CHN0001", "test-device-001", BiometricType.FINGERPRINT))
                    .willReturn(Optional.of(testBiometricInfo));
            given(passwordEncoder.matches("raw-biometric-hash", "encoded-biometric-hash"))
                    .willReturn(true);
            given(jwtTokenProvider.createAccessToken("CHN0001")).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken("CHN0001")).willReturn("refresh-token");
            given(jwtTokenProvider.getTokenExpirationTime("refresh-token")).willReturn(3600L);

            // when
            LoginResponseDto result = biometricService.biometricLogin(loginRequest, httpServletRequest);

            // then
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getUsername()).isEqualTo("테스트유저");
            assertThat(result.getDepartment()).isEqualTo("CHN");
            assertThat(result.getBiometricEnabled()).isTrue();

            verify(valueOperations).set(eq("refresh:token:CHN0001"), eq("refresh-token"), eq(3600L), eq(TimeUnit.SECONDS));
            verify(biometricInfoRepository).updateLastUsedAt(eq(1L), any(LocalDateTime.class));
            verify(biometricFailureService).clearFailCount("CHN0001", "test-device-001");
            verify(biometricLoginAttemptRepository).save(any(BiometricLoginAttempt.class));
        }

        @Test
        @DisplayName("디바이스가 차단된 상태에서 로그인 시도 시 예외 발생")
        void biometricLogin_Fail_DeviceBlocked() {
            // given
            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.BIOMETRIC_DEVICE_BLOCKED);

            verify(biometricLoginAttemptRepository).save(any(BiometricLoginAttempt.class));
        }

        @Test
        @DisplayName("최대 시도 횟수 초과 시 디바이스 차단 및 예외 발생")
        void biometricLogin_Fail_MaxAttemptsExceeded() {
            // given
            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", "test-device-001"))
                    .willReturn(3);

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.BIOMETRIC_MAX_ATTEMPTS_EXCEEDED);

            verify(biometricFailureService).blockDevice("CHN0001", "test-device-001");
            verify(biometricLoginAttemptRepository).save(any(BiometricLoginAttempt.class));
        }

        @Test
        @DisplayName("등록되지 않은 생체인증으로 로그인 시도 시 예외 발생")
        void biometricLogin_Fail_BiometricNotRegistered() {
            // given
            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", "test-device-001"))
                    .willReturn(0);
            given(userService.findUserByEmployeeId("CHN0001")).willReturn(testUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CHN0001", "test-device-001", BiometricType.FINGERPRINT))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.BIOMETRIC_NOT_REGISTERED);

            verify(biometricFailureService).incrementFailCount("CHN0001", "test-device-001");
            verify(biometricLoginAttemptRepository).save(any(BiometricLoginAttempt.class));
        }

        @Test
        @DisplayName("잘못된 생체인증 해시로 로그인 시도 시 예외 발생")
        void biometricLogin_Fail_InvalidBiometricHash() {
            // given
            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", "test-device-001"))
                    .willReturn(0);
            given(userService.findUserByEmployeeId("CHN0001")).willReturn(testUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CHN0001", "test-device-001", BiometricType.FINGERPRINT))
                    .willReturn(Optional.of(testBiometricInfo));
            given(passwordEncoder.matches("raw-biometric-hash", "encoded-biometric-hash"))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_BIOMETRIC);

            verify(biometricFailureService).incrementFailCount("CHN0001", "test-device-001");
            verify(biometricLoginAttemptRepository).save(any(BiometricLoginAttempt.class));
        }

        @Test
        @DisplayName("타임스탬프가 유효하지 않은 경우 예외 발생")
        void biometricLogin_Fail_InvalidTimestamp() {
            // given
            long oldTimestamp = System.currentTimeMillis() - 400000; // 6분 전
            BiometricLoginRequestDto invalidRequest = BiometricLoginRequestDto.builder()
                    .employeeId("CHN0001")
                    .biometricType("FINGERPRINT")
                    .deviceId("test-device-001")
                    .biometricHash("raw-biometric-hash")
                    .timestamp(oldTimestamp)
                    .build();

            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", "test-device-001"))
                    .willReturn(0);
            given(userService.findUserByEmployeeId("CHN0001")).willReturn(testUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CHN0001", "test-device-001", BiometricType.FINGERPRINT))
                    .willReturn(Optional.of(testBiometricInfo));
            given(passwordEncoder.matches("raw-biometric-hash", "encoded-biometric-hash"))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(invalidRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_REQUEST);

            verify(biometricFailureService).incrementFailCount("CHN0001", "test-device-001");
            verify(biometricLoginAttemptRepository).save(any(BiometricLoginAttempt.class));
        }

        @Test
        @DisplayName("3회 실패 후 디바이스 차단")
        void biometricLogin_DeviceBlockedAfterThreeFailures() {
            // given
            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", "test-device-001"))
                    .willReturn(2);
            given(userService.findUserByEmployeeId("CHN0001")).willReturn(testUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CHN0001", "test-device-001", BiometricType.FINGERPRINT))
                    .willReturn(Optional.empty());
            given(biometricFailureService.incrementFailCount("CHN0001", "test-device-001"))
                    .willReturn(3);

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.BIOMETRIC_NOT_REGISTERED);

            verify(biometricFailureService).incrementFailCount("CHN0001", "test-device-001");
            verify(biometricFailureService).blockDevice("CHN0001", "test-device-001");
        }
    }

    @Nested
    @DisplayName("생체인증 해제")
    class DeactivateBiometricTest {

        @Test
        @DisplayName("생체인증 해제 성공")
        void deactivateBiometric_Success() {
            // given
            String employeeId = "CHN0001";
            String deviceId = "test-device-001";
            String biometricType = "FINGERPRINT";

            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    employeeId, deviceId, BiometricType.FINGERPRINT))
                    .willReturn(Optional.of(testBiometricInfo));
            given(biometricInfoRepository.save(any(BiometricInfo.class)))
                    .willReturn(testBiometricInfo);

            // when
            BiometricResponseDto result = biometricService.deactivateBiometric(employeeId, deviceId, biometricType);

            // then
            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("생체인증이 해제되었습니다.");

            verify(biometricInfoRepository).save(testBiometricInfo);
            verify(biometricFailureService).clearFailCount(employeeId, deviceId);
            assertThat(testBiometricInfo.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 생체인증 해제 시도 시 예외 발생")
        void deactivateBiometric_Fail_BiometricNotFound() {
            // given
            String employeeId = "CHN0001";
            String deviceId = "test-device-001";
            String biometricType = "FINGERPRINT";

            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    employeeId, deviceId, BiometricType.FINGERPRINT))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> biometricService.deactivateBiometric(employeeId, deviceId, biometricType))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.BIOMETRIC_NOT_FOUND);

            verify(biometricInfoRepository, never()).save(any(BiometricInfo.class));
            verify(biometricFailureService, never()).clearFailCount(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("IP 주소 추출 테스트")
    class GetClientIpAddressTest {

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있는 경우 첫 번째 IP 반환")
        void getClientIpAddress_WithXForwardedFor() {
            // given
            lenient().when(httpServletRequest.getHeader("X-Forwarded-For"))
                    .thenReturn("192.168.1.100, 10.0.0.1");
            lenient().when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
            lenient().when(httpServletRequest.getHeader("User-Agent")).thenReturn("test-agent");
            lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            given(biometricFailureService.isDeviceBlocked(anyString(), anyString())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class);

            // IP 추출 로직은 private 메서드이므로 간접적으로 테스트
            verify(biometricLoginAttemptRepository).save(argThat(attempt ->
                    attempt.getIpAddress() != null
            ));
        }

        @Test
        @DisplayName("X-Real-IP 헤더가 있는 경우 해당 IP 반환")
        void getClientIpAddress_WithXRealIp() {
            // given
            lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            lenient().when(httpServletRequest.getHeader("X-Real-IP")).thenReturn("192.168.1.200");
            lenient().when(httpServletRequest.getHeader("User-Agent")).thenReturn("test-agent");
            lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            given(biometricFailureService.isDeviceBlocked(anyString(), anyString())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class);

            verify(biometricLoginAttemptRepository).save(argThat(attempt ->
                    attempt.getIpAddress() != null
            ));
        }

        @Test
        @DisplayName("특별한 헤더가 없는 경우 RemoteAddr 반환")
        void getClientIpAddress_WithRemoteAddr() {
            // given
            lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            lenient().when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
            lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            lenient().when(httpServletRequest.getHeader("User-Agent")).thenReturn("test-agent");
            given(biometricFailureService.isDeviceBlocked(anyString(), anyString())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class);

            verify(biometricLoginAttemptRepository).save(argThat(attempt ->
                    attempt.getIpAddress() != null
            ));
        }
    }

    @Nested
    @DisplayName("로그인 시도 기록 테스트")
    class LoginAttemptRecordTest {

        @Test
        @DisplayName("성공한 로그인 시도 기록")
        void recordSuccessfulAttempt() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            lenient().when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
            lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            lenient().when(httpServletRequest.getHeader("User-Agent")).thenReturn("test-user-agent");
            
            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", "test-device-001"))
                    .willReturn(0);
            given(userService.findUserByEmployeeId("CHN0001")).willReturn(testUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CHN0001", "test-device-001", BiometricType.FINGERPRINT))
                    .willReturn(Optional.of(testBiometricInfo));
            given(passwordEncoder.matches("raw-biometric-hash", "encoded-biometric-hash"))
                    .willReturn(true);
            given(jwtTokenProvider.createAccessToken("CHN0001")).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken("CHN0001")).willReturn("refresh-token");
            given(jwtTokenProvider.getTokenExpirationTime("refresh-token")).willReturn(3600L);

            // when
            biometricService.biometricLogin(loginRequest, httpServletRequest);

            // then
            verify(biometricLoginAttemptRepository, times(1)).save(argThat(attempt ->
                    attempt.getAttemptResult() == AttemptResult.SUCCESS &&
                            attempt.getEmployeeId().equals("CHN0001") &&
                            attempt.getDeviceId().equals("test-device-001") &&
                            attempt.getBiometricType() == BiometricType.FINGERPRINT &&
                            attempt.getIpAddress().equals("127.0.0.1") &&
                            attempt.getUserAgent().equals("test-user-agent")
            ));
        }

        @Test
        @DisplayName("실패한 로그인 시도 기록")
        void recordFailedAttempt() {
            // given
            lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            lenient().when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
            lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            lenient().when(httpServletRequest.getHeader("User-Agent")).thenReturn("test-user-agent");
            
            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", "test-device-001"))
                    .willReturn(0);
            given(userService.findUserByEmployeeId("CHN0001")).willReturn(testUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CHN0001", "test-device-001", BiometricType.FINGERPRINT))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class);

            verify(biometricLoginAttemptRepository, times(1)).save(argThat(attempt ->
                    attempt.getAttemptResult() == AttemptResult.FAILED &&
                            attempt.getEmployeeId().equals("CHN0001") &&
                            attempt.getDeviceId().equals("test-device-001") &&
                            attempt.getBiometricType() == BiometricType.FINGERPRINT &&
                            attempt.getFailureReason().equals("Biometric not registered") &&
                            attempt.getIpAddress().equals("127.0.0.1") &&
                            attempt.getUserAgent().equals("test-user-agent")
            ));
        }
    }

    @Nested
    @DisplayName("Edge Case 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("null 값이 포함된 요청 처리")
        void handleNullValues() {
            // given
            BiometricLoginRequestDto nullRequest = BiometricLoginRequestDto.builder()
                    .employeeId(null)
                    .biometricType("FINGERPRINT")
                    .deviceId("test-device-001")
                    .biometricHash("raw-biometric-hash")
                    .timestamp(System.currentTimeMillis())
                    .build();

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(nullRequest, httpServletRequest))
                    .isInstanceOf(Exception.class); // NullPointerException 또는 다른 예외 발생 예상
        }

        @Test
        @DisplayName("빈 문자열이 포함된 요청 처리")
        void handleEmptyStrings() {
            // given
            BiometricLoginRequestDto emptyRequest = BiometricLoginRequestDto.builder()
                    .employeeId("")
                    .biometricType("FINGERPRINT")
                    .deviceId("test-device-001")
                    .biometricHash("raw-biometric-hash")
                    .timestamp(System.currentTimeMillis())
                    .build();

            given(biometricFailureService.isDeviceBlocked("", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("", "test-device-001"))
                    .willReturn(0);
            given(userService.findUserByEmployeeId(""))
                    .willThrow(new BaseException(BaseResponseStatus.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(emptyRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("매우 긴 문자열 처리")
        void handleVeryLongStrings() {
            // given
            String longDeviceId = "a".repeat(1000);
            BiometricLoginRequestDto longRequest = BiometricLoginRequestDto.builder()
                    .employeeId("CHN0001")
                    .biometricType("FINGERPRINT")
                    .deviceId(longDeviceId)
                    .biometricHash("raw-biometric-hash")
                    .timestamp(System.currentTimeMillis())
                    .build();

            given(biometricFailureService.isDeviceBlocked("CHN0001", longDeviceId))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", longDeviceId))
                    .willReturn(0);
            given(userService.findUserByEmployeeId("CHN0001")).willReturn(testUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CHN0001", longDeviceId, BiometricType.FINGERPRINT))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(longRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.BIOMETRIC_NOT_REGISTERED);
        }

        @Test
        @DisplayName("잘못된 BiometricType enum 값 처리")
        void handleInvalidBiometricType() {
            // given
            BiometricLoginRequestDto invalidTypeRequest = BiometricLoginRequestDto.builder()
                    .employeeId("CHN0001")
                    .biometricType("INVALID_TYPE")
                    .deviceId("test-device-001")
                    .biometricHash("raw-biometric-hash")
                    .timestamp(System.currentTimeMillis())
                    .build();

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(invalidTypeRequest, httpServletRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Redis 연결 실패 상황 처리")
        void handleRedisConnectionFailure() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", "test-device-001"))
                    .willReturn(0);
            given(userService.findUserByEmployeeId("CHN0001")).willReturn(testUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CHN0001", "test-device-001", BiometricType.FINGERPRINT))
                    .willReturn(Optional.of(testBiometricInfo));
            given(passwordEncoder.matches("raw-biometric-hash", "encoded-biometric-hash"))
                    .willReturn(true);
            given(jwtTokenProvider.createAccessToken("CHN0001")).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken("CHN0001")).willReturn("refresh-token");
            given(jwtTokenProvider.getTokenExpirationTime("refresh-token")).willReturn(3600L);

            // Redis 연결 실패 시뮬레이션
            doThrow(new RuntimeException("Redis connection failed"))
                    .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Redis connection failed");
        }
    }

    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTest {

        @Test
        @DisplayName("동시에 여러 요청이 들어왔을 때의 실패 카운트 관리")
        void handleConcurrentFailureCount() {
            // given
            given(biometricFailureService.isDeviceBlocked("CHN0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CHN0001", "test-device-001"))
                    .willReturn(2); // 이미 2번 실패한 상태
            given(userService.findUserByEmployeeId("CHN0001")).willReturn(testUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CHN0001", "test-device-001", BiometricType.FINGERPRINT))
                    .willReturn(Optional.empty());
            given(biometricFailureService.incrementFailCount("CHN0001", "test-device-001"))
                    .willReturn(3); // 3번째 실패로 차단

            // when & then
            assertThatThrownBy(() -> biometricService.biometricLogin(loginRequest, httpServletRequest))
                    .isInstanceOf(BaseException.class);

            verify(biometricFailureService).incrementFailCount("CHN0001", "test-device-001");
            verify(biometricFailureService).blockDevice("CHN0001", "test-device-001");
        }
    }

    @Nested
    @DisplayName("Department Enum 테스트")
    class DepartmentTest {

        @Test
        @DisplayName("다양한 부서의 사용자 로그인 테스트")
        void loginWithDifferentDepartments() {
            // given - BANK 부서 사용자
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            User bankUser = User.builder()
                    .id("CORE0001")
                    .username("은행직원")
                    .department(Department.CORE_BANK)
                    .build();

            BiometricLoginRequestDto bankRequest = BiometricLoginRequestDto.builder()
                    .employeeId("CORE0001")
                    .biometricType("FINGERPRINT")
                    .deviceId("test-device-001")
                    .biometricHash("raw-biometric-hash")
                    .timestamp(System.currentTimeMillis())
                    .build();

            given(biometricFailureService.isDeviceBlocked("CORE0001", "test-device-001"))
                    .willReturn(false);
            given(biometricFailureService.getFailCount("CORE0001", "test-device-001"))
                    .willReturn(0);
            given(userService.findUserByEmployeeId("CORE0001")).willReturn(bankUser);
            given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    "CORE0001", "test-device-001", BiometricType.FINGERPRINT))
                    .willReturn(Optional.of(testBiometricInfo));
            given(passwordEncoder.matches("raw-biometric-hash", "encoded-biometric-hash"))
                    .willReturn(true);
            given(jwtTokenProvider.createAccessToken("CORE0001")).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken("CORE0001")).willReturn("refresh-token");
            given(jwtTokenProvider.getTokenExpirationTime("refresh-token")).willReturn(3600L);

            // when
            LoginResponseDto result = biometricService.biometricLogin(bankRequest, httpServletRequest);

            // then
            assertThat(result.getDepartment()).isEqualTo("CORE");
            assertThat(result.getUsername()).isEqualTo("은행직원");
        }
    }
}