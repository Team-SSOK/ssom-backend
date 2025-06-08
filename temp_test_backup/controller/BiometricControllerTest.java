package kr.ssok.ssom.backend.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ssok.ssom.backend.domain.user.dto.*;
import kr.ssok.ssom.backend.domain.user.service.BiometricService;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BiometricController.class)
@DisplayName("생체인증 컨트롤러 테스트")
class BiometricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BiometricService biometricService;

    @Test
    @DisplayName("생체인증 상태 확인 API - 성공")
    void checkBiometricStatus_ShouldReturnStatus() throws Exception {
        // given
        String employeeId = "CORE0001";
        BiometricStatusDto statusDto = BiometricStatusDto.builder()
            .isRegistered(true)
            .availableTypes(Arrays.asList("FINGERPRINT"))
            .deviceCount(1)
            .lastUsedAt(LocalDateTime.now())
            .build();

        given(biometricService.checkBiometricStatus(employeeId)).willReturn(statusDto);

        // when & then
        mockMvc.perform(get("/api/biometric/status/{employeeId}", employeeId))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result.isRegistered").value(true))
            .andExpect(jsonPath("$.result.availableTypes[0]").value("FINGERPRINT"))
            .andExpect(jsonPath("$.result.deviceCount").value(1));
    }

    @Test
    @DisplayName("생체인증 등록 API - 성공")
    void registerBiometric_WithValidRequest_ShouldRegisterSuccessfully() throws Exception {
        // given
        BiometricRegistrationRequestDto request = BiometricRegistrationRequestDto.builder()
            .employeeId("CORE0001")
            .biometricType("FINGERPRINT")
            .deviceId("test-device-123")
            .biometricHash("test-hash")
            .deviceInfo("{\"model\":\"Test Phone\"}")
            .build();

        BiometricResponseDto response = BiometricResponseDto.builder()
            .success(true)
            .message("생체인증이 성공적으로 등록되었습니다.")
            .biometricId(1L)
            .build();

        given(biometricService.registerBiometric(any(BiometricRegistrationRequestDto.class)))
            .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/biometric/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result.success").value(true))
            .andExpect(jsonPath("$.result.biometricId").value(1));
    }

    @Test
    @DisplayName("생체인증 등록 API - 유효성 검증 실패")
    void registerBiometric_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // given
        BiometricRegistrationRequestDto request = BiometricRegistrationRequestDto.builder()
            // employeeId 누락 - 유효성 검증 실패 케이스
            .biometricType("FINGERPRINT")
            .deviceId("test-device-123")
            .biometricHash("test-hash")
            .build();

        // when & then
        mockMvc.perform(post("/api/biometric/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("생체인증 로그인 API - 성공")
    void biometricLogin_WithValidCredentials_ShouldLoginSuccessfully() throws Exception {
        // given
        BiometricLoginRequestDto request = BiometricLoginRequestDto.builder()
            .employeeId("CORE0001")
            .biometricType("FINGERPRINT")
            .deviceId("test-device-123")
            .biometricHash("test-hash")
            .timestamp(System.currentTimeMillis())
            .build();

        LoginResponseDto response = LoginResponseDto.builder()
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .username("홍길동")
            .department("CORE")
            .build();

        given(biometricService.biometricLogin(any(BiometricLoginRequestDto.class), any()))
            .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/biometric/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result.accessToken").value("access-token"))
            .andExpect(jsonPath("$.result.username").value("홍길동"));
    }

    @Test
    @DisplayName("생체인증 로그인 API - 최대 시도 횟수 초과")
    void biometricLogin_WithMaxAttemptsExceeded_ShouldReturnLocked() throws Exception {
        // given
        BiometricLoginRequestDto request = BiometricLoginRequestDto.builder()
            .employeeId("CORE0001")
            .biometricType("FINGERPRINT")
            .deviceId("test-device-123")
            .biometricHash("test-hash")
            .timestamp(System.currentTimeMillis())
            .build();

        given(biometricService.biometricLogin(any(BiometricLoginRequestDto.class), any()))
            .willThrow(new BaseException(BaseResponseStatus.BIOMETRIC_MAX_ATTEMPTS_EXCEEDED));

        // when & then
        mockMvc.perform(post("/api/biometric/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    @DisplayName("생체인증 해제 API - 성공")
    void deactivateBiometric_WithValidRequest_ShouldDeactivateSuccessfully() throws Exception {
        // given
        String employeeId = "CORE0001";
        String deviceId = "test-device-123";
        String biometricType = "FINGERPRINT";

        BiometricResponseDto response = BiometricResponseDto.builder()
            .success(true)
            .message("생체인증이 해제되었습니다.")
            .build();

        given(biometricService.deactivateBiometric(employeeId, deviceId, biometricType))
            .willReturn(response);

        // when & then
        mockMvc.perform(delete("/api/biometric/deactivate")
                .param("employeeId", employeeId)
                .param("deviceId", deviceId)
                .param("biometricType", biometricType))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result.success").value(true));
    }

    @Test
    @DisplayName("생체인증 헬스체크 API")
    void healthCheck_ShouldReturnOk() throws Exception {
        // when & then
        mockMvc.perform(get("/api/biometric/health"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result").value("Biometric API is running"));
    }
}
