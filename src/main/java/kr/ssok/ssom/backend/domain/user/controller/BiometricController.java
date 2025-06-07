package kr.ssok.ssom.backend.domain.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ssok.ssom.backend.domain.user.dto.*;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import kr.ssok.ssom.backend.domain.user.service.BiometricService;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/biometric")
@RequiredArgsConstructor
@Slf4j
public class BiometricController {

    private final BiometricService biometricService;

    /**
     * 생체인증 상태 확인
     * GET /api/biometric/status
     */
    @GetMapping("/status")
    public ResponseEntity<BaseResponse<BiometricStatusDto>> checkBiometricStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("생체인증 상태 확인 API 호출: employeeId={}", userPrincipal.getEmployeeId());
        
        BiometricStatusDto response = biometricService.checkBiometricStatus(userPrincipal.getEmployeeId());
        
        return ResponseEntity.ok(
            new BaseResponse<>(BaseResponseStatus.SUCCESS, response)
        );
    }

    /**
     * 생체인증 등록
     * POST /api/biometric/register
     */
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<BiometricResponseDto>> registerBiometric(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody BiometricRegistrationRequestDto request) {
        
        log.info("생체인증 등록 API 호출: employeeId={}, type={}", 
            userPrincipal.getEmployeeId(), request.getBiometricType());
        
        BiometricResponseDto response = biometricService.registerBiometric(userPrincipal.getEmployeeId(), request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new BaseResponse<>(BaseResponseStatus.CREATED, response));
    }

    /**
     * ID 기반 생체인증 로그인
     * POST /api/biometric/login
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponseDto>> biometricLogin(
            @Valid @RequestBody BiometricLoginRequestDto request,
            HttpServletRequest httpRequest) {
        
        log.info("생체인증 로그인 API 호출: employeeId={}, deviceId={}", 
            request.getEmployeeId(), request.getDeviceId());
        
        try {
            LoginResponseDto response = biometricService.biometricLogin(request, httpRequest);
            
            return ResponseEntity.ok(
                new BaseResponse<>(BaseResponseStatus.LOGIN_SUCCESS, response)
            );
            
        } catch (Exception e) {
            log.error("생체인증 로그인 실패: {}", e.getMessage(), e);
            
            // BaseException에서 구체적인 예외 타입 확인
            if (e instanceof BaseException) {
                BaseException baseException = (BaseException) e;
                BaseResponseStatus status = baseException.getStatus();
                
                // 디바이스 차단 상태
                if (status == BaseResponseStatus.BIOMETRIC_DEVICE_BLOCKED) {
                    return ResponseEntity.status(HttpStatus.LOCKED)
                        .body(new BaseResponse<>(BaseResponseStatus.BIOMETRIC_DEVICE_BLOCKED));
                }
                
                // 최대 시도 횟수 초과
                if (status == BaseResponseStatus.BIOMETRIC_MAX_ATTEMPTS_EXCEEDED) {
                    return ResponseEntity.status(HttpStatus.LOCKED)
                        .body(new BaseResponse<>(BaseResponseStatus.BIOMETRIC_MAX_ATTEMPTS_EXCEEDED));
                }
                
                // 기타 생체인증 관련 오류
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(status));
            }
            
            // 일반적인 예외
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new BaseResponse<>(BaseResponseStatus.LOGIN_FAILED));
        }
    }

    /**
     * 생체인증 해제
     * DELETE /api/biometric/deactivate
     */
    @DeleteMapping("/deactivate")
    public ResponseEntity<BaseResponse<BiometricResponseDto>> deactivateBiometric(
            @RequestParam String employeeId,
            @RequestParam String deviceId,
            @RequestParam String biometricType,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("생체인증 해제 API 호출: employeeId={}, deviceId={}, type={}, requestBy={}", 
            employeeId, deviceId, biometricType, userPrincipal.getEmployeeId());
        
        // 본인 확인 (본인만 해제 가능)
        if (!employeeId.equals(userPrincipal.getEmployeeId())) {
            log.warn("생체인증 해제 권한 없음 - target: {}, requester: {}", employeeId, userPrincipal.getEmployeeId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new BaseResponse<>(BaseResponseStatus.FORBIDDEN));
        }
        
        BiometricResponseDto response = biometricService.deactivateBiometric(
            employeeId, deviceId, biometricType);
        
        return ResponseEntity.ok(
            new BaseResponse<>(BaseResponseStatus.SUCCESS, response)
        );
    }

    /**
     * 생체인증 로그인 기록 조회 (관리자용)
     * GET /api/biometric/attempts/{employeeId}
     */
    @GetMapping("/attempts/{employeeId}")
    public ResponseEntity<BaseResponse<?>> getBiometricAttempts(
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("생체인증 기록 조회 API 호출: employeeId={}, page={}, size={}", 
            employeeId, page, size);
        
        // TODO: 관리자 권한 체크 및 페이징 처리된 조회 로직 구현
        // 현재는 기본 응답만 반환
        return ResponseEntity.ok(
            new BaseResponse<>(BaseResponseStatus.SUCCESS, "조회 기능 개발 예정")
        );
    }

    /**
     * 헬스체크용 엔드포인트
     * GET /api/biometric/health
     */
    @GetMapping("/health")
    public ResponseEntity<BaseResponse<String>> healthCheck() {
        return ResponseEntity.ok(
            new BaseResponse<>(BaseResponseStatus.SUCCESS, "Biometric API is running")
        );
    }
}
