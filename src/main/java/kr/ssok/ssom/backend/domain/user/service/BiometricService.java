package kr.ssok.ssom.backend.domain.user.service;

import jakarta.servlet.http.HttpServletRequest;
import kr.ssok.ssom.backend.domain.user.dto.*;

public interface BiometricService {

    /**
     * 생체인증 상태 확인 (사용자 ID 기반)
     */
    BiometricStatusDto checkBiometricStatus(String employeeId);

    /**
     * 생체인증 등록
     */
    BiometricResponseDto registerBiometric(String employeeId, BiometricRegistrationRequestDto request);

    /**
     * ID 기반 생체인증 로그인 (권장 방식)
     */
    LoginResponseDto biometricLogin(BiometricLoginRequestDto request, HttpServletRequest httpRequest);

    /**
     * 생체인증 해제 (비활성화)
     */
    BiometricResponseDto deactivateBiometric(String employeeId, String deviceId, String biometricType);
}
