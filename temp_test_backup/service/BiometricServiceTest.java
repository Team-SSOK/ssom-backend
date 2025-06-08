package kr.ssok.ssom.backend.domain.user.service;

import kr.ssok.ssom.backend.domain.user.dto.*;
import kr.ssok.ssom.backend.domain.user.entity.*;
import kr.ssok.ssom.backend.domain.user.repository.BiometricInfoRepository;
import kr.ssok.ssom.backend.domain.user.repository.BiometricLoginAttemptRepository;
import kr.ssok.ssom.backend.domain.user.security.jwt.JwtTokenProvider;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("생체인증 서비스 테스트")
class BiometricServiceTest {

    @InjectMocks
    private BiometricService biometricService;

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

    private User testUser;
    private BiometricInfo testBiometricInfo;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id("CORE0001")
            .username("홍길동")
            .password("encodedPassword")
            .phoneNumber("010-1234-5678")
            .department(Department.CORE)
            .build();

        testBiometricInfo = BiometricInfo.builder()
            .id(1L)
            .employeeId("CORE0001")
            .biometricType(BiometricType.FINGERPRINT)
            .deviceId("test-device-123")
            .biometricHash("encoded-biometric-hash")
            .isActive(true)
            .lastUsedAt(LocalDateTime.now())
            .build();

        mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("127.0.0.1");
        mockRequest.addHeader("User-Agent", "Test-Agent");
    }

    @Test
    @DisplayName("생체인증 상태 확인 - 등록된 생체인증이 있는 경우")
    void checkBiometricStatus_WithRegisteredBiometric_ShouldReturnRegisteredStatus() {
        // given
        String employeeId = "CORE0001";
        List<BiometricInfo> biometrics = Arrays.asList(testBiometricInfo);
        
        given(biometricInfoRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
            .willReturn(biometrics);

        // when
        BiometricStatusDto result = biometricService.checkBiometricStatus(employeeId);

        // then
        assertThat(result.getIsRegistered()).isTrue();
        assertThat(result.getAvailableTypes()).contains("FINGERPRINT");
        assertThat(result.getDeviceCount()).isEqualTo(1);
        assertThat(result.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("생체인증 등록 - 정상적인 경우")
    void registerBiometric_WithValidRequest_ShouldRegisterSuccessfully() {
        // given
        BiometricRegistrationRequestDto request = BiometricRegistrationRequestDto.builder()
            .employeeId("CORE0001")
            .biometricType("FINGERPRINT")
            .deviceId("test-device-123")
            .biometricHash("raw-biometric-hash")
            .deviceInfo("{\"model\":\"Test Phone\"}")
            .build();

        given(userService.findUserByEmployeeId("CORE0001")).willReturn(testUser);
        given(biometricInfoRepository.existsByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
            anyString(), anyString(), any(BiometricType.class))).willReturn(false);
        given(passwordEncoder.encode("raw-biometric-hash")).willReturn("encoded-biometric-hash");
        given(biometricInfoRepository.save(any(BiometricInfo.class))).willReturn(testBiometricInfo);

        // when
        BiometricResponseDto result = biometricService.registerBiometric(request);

        // then
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getMessage()).contains("성공적으로 등록");
        assertThat(result.getBiometricId()).isEqualTo(1L);
        
        verify(biometricInfoRepository).save(any(BiometricInfo.class));
    }

    @Test
    @DisplayName("생체인증 등록 - 이미 등록된 경우")
    void registerBiometric_WithAlreadyRegistered_ShouldThrowException() {
        // given
        BiometricRegistrationRequestDto request = BiometricRegistrationRequestDto.builder()
            .employeeId("CORE0001")
            .biometricType("FINGERPRINT")
            .deviceId("test-device-123")
            .biometricHash("raw-biometric-hash")
            .build();

        given(userService.findUserByEmployeeId("CORE0001")).willReturn(testUser);
        given(biometricInfoRepository.existsByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
            anyString(), anyString(), any(BiometricType.class))).willReturn(true);

        // when & then
        assertThatThrownBy(() -> biometricService.registerBiometric(request))
            .isInstanceOf(BaseException.class)
            .hasFieldOrPropertyWithValue("status", BaseResponseStatus.BIOMETRIC_ALREADY_REGISTERED);
    }

    @Test
    @DisplayName("생체인증 로그인 - 성공적인 경우")
    void biometricLogin_WithValidCredentials_ShouldLoginSuccessfully() {
        // given
        BiometricLoginRequestDto request = BiometricLoginRequestDto.builder()
            .employeeId("CORE0001")
            .biometricType("FINGERPRINT")
            .deviceId("test-device-123")
            .biometricHash("raw-biometric-hash")
            .timestamp(System.currentTimeMillis())
            .build();

        given(biometricFailureService.getFailCount(anyString(), anyString())).willReturn(0);
        given(userService.findUserByEmployeeId("CORE0001")).willReturn(testUser);
        given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
            anyString(), anyString(), any(BiometricType.class)))
            .willReturn(Optional.of(testBiometricInfo));
        given(passwordEncoder.matches("raw-biometric-hash", "encoded-biometric-hash")).willReturn(true);
        given(jwtTokenProvider.createAccessToken("CORE0001")).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken("CORE0001")).willReturn("refresh-token");

        // when
        LoginResponseDto result = biometricService.biometricLogin(request, mockRequest);

        // then
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getUsername()).isEqualTo("홍길동");
        assertThat(result.getDepartment()).isEqualTo("CORE");

        verify(biometricFailureService).clearFailCount("CORE0001", "test-device-123");
        verify(biometricInfoRepository).save(any(BiometricInfo.class));
    }

    @Test
    @DisplayName("생체인증 로그인 - 최대 시도 횟수 초과")
    void biometricLogin_WithMaxAttemptsExceeded_ShouldThrowException() {
        // given
        BiometricLoginRequestDto request = BiometricLoginRequestDto.builder()
            .employeeId("CORE0001")
            .biometricType("FINGERPRINT")
            .deviceId("test-device-123")
            .biometricHash("raw-biometric-hash")
            .timestamp(System.currentTimeMillis())
            .build();

        given(biometricFailureService.getFailCount(anyString(), anyString())).willReturn(3);

        // when & then
        assertThatThrownBy(() -> biometricService.biometricLogin(request, mockRequest))
            .isInstanceOf(BaseException.class)
            .hasFieldOrPropertyWithValue("status", BaseResponseStatus.BIOMETRIC_MAX_ATTEMPTS_EXCEEDED);

        verify(biometricLoginAttemptRepository).save(any(BiometricLoginAttempt.class));
    }

    @Test
    @DisplayName("생체인증 로그인 - 잘못된 생체인증 해시")
    void biometricLogin_WithInvalidHash_ShouldThrowException() {
        // given
        BiometricLoginRequestDto request = BiometricLoginRequestDto.builder()
            .employeeId("CORE0001")
            .biometricType("FINGERPRINT")
            .deviceId("test-device-123")
            .biometricHash("wrong-hash")
            .timestamp(System.currentTimeMillis())
            .build();

        given(biometricFailureService.getFailCount(anyString(), anyString())).willReturn(0);
        given(userService.findUserByEmployeeId("CORE0001")).willReturn(testUser);
        given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
            anyString(), anyString(), any(BiometricType.class)))
            .willReturn(Optional.of(testBiometricInfo));
        given(passwordEncoder.matches("wrong-hash", "encoded-biometric-hash")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> biometricService.biometricLogin(request, mockRequest))
            .isInstanceOf(BaseException.class)
            .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_BIOMETRIC);

        verify(biometricFailureService).incrementFailCount("CORE0001", "test-device-123");
        verify(biometricLoginAttemptRepository).save(any(BiometricLoginAttempt.class));
    }

    @Test
    @DisplayName("생체인증 해제 - 정상적인 경우")
    void deactivateBiometric_WithValidRequest_ShouldDeactivateSuccessfully() {
        // given
        String employeeId = "CORE0001";
        String deviceId = "test-device-123";
        String biometricType = "FINGERPRINT";

        given(biometricInfoRepository.findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
            employeeId, deviceId, BiometricType.valueOf(biometricType)))
            .willReturn(Optional.of(testBiometricInfo));

        // when
        BiometricResponseDto result = biometricService.deactivateBiometric(employeeId, deviceId, biometricType);

        // then
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getMessage()).contains("해제되었습니다");

        verify(biometricInfoRepository).save(argThat(info -> !info.getIsActive()));
        verify(biometricFailureService).clearFailCount(employeeId, deviceId);
    }
}
