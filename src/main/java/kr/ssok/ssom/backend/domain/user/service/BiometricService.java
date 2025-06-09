package kr.ssok.ssom.backend.domain.user.service;

import jakarta.servlet.http.HttpServletRequest;
import kr.ssok.ssom.backend.domain.user.dto.*;
import kr.ssok.ssom.backend.domain.user.entity.*;
import kr.ssok.ssom.backend.domain.user.repository.BiometricInfoRepository;
import kr.ssok.ssom.backend.domain.user.repository.BiometricLoginAttemptRepository;
import kr.ssok.ssom.backend.domain.user.security.jwt.JwtTokenProvider;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BiometricService {

    private final BiometricInfoRepository biometricInfoRepository;
    private final BiometricLoginAttemptRepository biometricLoginAttemptRepository;
    private final BiometricFailureService biometricFailureService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    // Refresh Token을 저장하는 키 prefix
    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";

    /**
     * 생체인증 상태 확인 (사용자 ID 기반)
     */
    @Transactional(readOnly = true)
    public BiometricStatusDto checkBiometricStatus(String employeeId) {
        log.info("생체인증 상태 확인 요청: employeeId={}", employeeId);
        
        // 해당 사용자의 생체인증 등록 여부 확인
        List<BiometricInfo> biometrics = biometricInfoRepository
            .findByEmployeeIdAndIsActiveTrue(employeeId);
        
        return BiometricStatusDto.builder()
            .isRegistered(!biometrics.isEmpty())
            .availableTypes(biometrics.stream()
                .map(b -> b.getBiometricType().name())
                .collect(Collectors.toList()))
            .deviceCount(biometrics.size())
            .lastUsedAt(biometrics.stream()
                .map(BiometricInfo::getLastUsedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null))
            .build();
    }

    /**
     * 생체인증 등록
     */
    public BiometricResponseDto registerBiometric(String employeeId, BiometricRegistrationRequestDto request) {
        log.info("생체인증 등록 요청: employeeId={}, type={}, deviceId={}",
                employeeId, request.getBiometricType(), request.getDeviceId());

        // 1. 사용자 존재 여부 확인
        User user = userService.findUserByEmployeeId(employeeId);

        // 2. 기존 등록 정보 확인
        boolean exists = biometricInfoRepository.existsByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                employeeId,
            request.getDeviceId(),
            BiometricType.valueOf(request.getBiometricType())
        );

        if (exists) {
            throw new BaseException(BaseResponseStatus.BIOMETRIC_ALREADY_REGISTERED);
        }

        // 3. 생체인증 해시 암호화 (추가 보안)
        String encryptedHash = passwordEncoder.encode(request.getBiometricHash());

        // 4. 생체인증 정보 저장
        BiometricInfo biometricInfo = BiometricInfo.builder()
            .employeeId(employeeId)
            .biometricType(BiometricType.valueOf(request.getBiometricType()))
            .deviceId(request.getDeviceId())
            .biometricHash(encryptedHash)
            .deviceInfo(request.getDeviceInfo())
            .isActive(true)
            .build();

        BiometricInfo saved = biometricInfoRepository.save(biometricInfo);

        log.info("생체인증 등록 완료: id={}", saved.getId());

        return BiometricResponseDto.builder()
            .success(true)
            .message("생체인증이 성공적으로 등록되었습니다.")
            .biometricId(saved.getId())
            .build();
    }

    /**
     * ID 기반 생체인증 로그인 (권장 방식)
     */
    public LoginResponseDto biometricLogin(BiometricLoginRequestDto request, HttpServletRequest httpRequest) {
        String employeeId = request.getEmployeeId();
        String deviceId = request.getDeviceId();
        
        log.info("생체인증 로그인 시도: employeeId={}, deviceId={}", employeeId, deviceId);

        try {
            // 1. 디바이스 차단 상태 확인
            if (biometricFailureService.isDeviceBlocked(employeeId, deviceId)) {
                recordFailedAttempt(employeeId, request, "Device blocked", httpRequest);
                throw new BaseException(BaseResponseStatus.BIOMETRIC_DEVICE_BLOCKED);
            }

            // 2. 현재 실패 횟수 확인
            int currentFailCount = biometricFailureService.getFailCount(employeeId, deviceId);
            if (currentFailCount >= 3) {
                // 3회 이상 실패 시 디바이스 차단
                biometricFailureService.blockDevice(employeeId, deviceId);
                recordFailedAttempt(employeeId, request, "Max attempts exceeded - device blocked", httpRequest);
                throw new BaseException(BaseResponseStatus.BIOMETRIC_MAX_ATTEMPTS_EXCEEDED);
            }

            // 3. 사용자 존재 여부 확인
            User user = userService.findUserByEmployeeId(employeeId);

            // 4. 해당 사용자의 해당 디바이스 생체인증 정보 조회
            BiometricInfo biometricInfo = biometricInfoRepository
                .findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                    employeeId, 
                    deviceId,
                    BiometricType.valueOf(request.getBiometricType())
                )
                .orElseThrow(() -> {
                    recordFailedAttempt(employeeId, request, "Biometric not registered", httpRequest);
                    return new BaseException(BaseResponseStatus.BIOMETRIC_NOT_REGISTERED);
                });

            // 5. 생체인증 해시 검증
            if (!verifyBiometricHash(request.getBiometricHash(), biometricInfo.getBiometricHash())) {
                recordFailedAttempt(employeeId, request, "Invalid biometric hash", httpRequest);
                throw new BaseException(BaseResponseStatus.INVALID_BIOMETRIC);
            }

            // 6. 타임스탬프 검증 (Replay Attack 방지)
            long currentTime = System.currentTimeMillis();
            long requestTime = request.getTimestamp();
            if (Math.abs(currentTime - requestTime) > 300000) { // 5분 초과
                recordFailedAttempt(employeeId, request, "Invalid timestamp", httpRequest);
                throw new BaseException(BaseResponseStatus.INVALID_REQUEST);
            }

            // 7. JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(user.getId());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

            // 토큰 만료 시간 계산
            long expirationTime = jwtTokenProvider.getTokenExpirationTime(refreshToken);

            // Refresh Token을 Redis에 저장
            String refreshTokenKey = REFRESH_TOKEN_PREFIX + user.getId();
            redisTemplate.opsForValue().set(
                    refreshTokenKey,
                    refreshToken,
                    expirationTime,
                    TimeUnit.SECONDS
            );

            // 8. 마지막 사용 시간 업데이트 (전용 메서드 사용)
            LocalDateTime now = LocalDateTime.now();
            biometricInfoRepository.updateLastUsedAt(biometricInfo.getId(), now);

            // 9. 성공 기록
            recordSuccessfulAttempt(employeeId, request, httpRequest);

            // 10. 실패 횟수 초기화
            biometricFailureService.clearFailCount(employeeId, deviceId);

            log.info("생체인증 로그인 성공: employeeId={}", employeeId);

            return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .department(user.getDepartment().getPrefix())
                .expiresIn(expirationTime)
                .biometricEnabled(true) // 생체인증으로 로그인했으므로 true
                .lastLoginAt(now.toString())
                .build();

        } catch (BaseException e) {
            // 실패 횟수 증가
            int newFailCount = biometricFailureService.incrementFailCount(employeeId, deviceId);
            
            // 3회 실패 시 디바이스 차단
            if (newFailCount >= 3) {
                biometricFailureService.blockDevice(employeeId, deviceId);
                log.warn("디바이스 차단됨 - 사원번호: {}, 디바이스: {}, 실패횟수: {}", employeeId, deviceId, newFailCount);
            }
            
            throw e;
        }
    }

    /**
     * 생체인증 해시 검증
     */
    private boolean verifyBiometricHash(String inputHash, String storedHash) {
        return passwordEncoder.matches(inputHash, storedHash);
    }

    /**
     * 성공한 로그인 시도 기록
     */
    private void recordSuccessfulAttempt(String employeeId, BiometricLoginRequestDto request, HttpServletRequest httpRequest) {
        BiometricLoginAttempt attempt = BiometricLoginAttempt.builder()
            .employeeId(employeeId)
            .deviceId(request.getDeviceId())
            .biometricType(BiometricType.valueOf(request.getBiometricType()))
            .attemptResult(AttemptResult.SUCCESS)
            .ipAddress(getClientIpAddress(httpRequest))
            .userAgent(httpRequest.getHeader("User-Agent"))
            .build();

        biometricLoginAttemptRepository.save(attempt);
    }

    /**
     * 실패한 로그인 시도 기록
     */
    private void recordFailedAttempt(String employeeId, BiometricLoginRequestDto request, String reason, HttpServletRequest httpRequest) {
        BiometricLoginAttempt attempt = BiometricLoginAttempt.builder()
            .employeeId(employeeId)
            .deviceId(request.getDeviceId())
            .biometricType(BiometricType.valueOf(request.getBiometricType()))
            .attemptResult(AttemptResult.FAILED)
            .failureReason(reason)
            .ipAddress(getClientIpAddress(httpRequest))
            .userAgent(httpRequest.getHeader("User-Agent"))
            .build();

        biometricLoginAttemptRepository.save(attempt);
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 생체인증 해제 (비활성화)
     */
    public BiometricResponseDto deactivateBiometric(String employeeId, String deviceId, String biometricType) {
        log.info("생체인증 해제 요청: employeeId={}, deviceId={}, type={}", employeeId, deviceId, biometricType);

        BiometricInfo biometricInfo = biometricInfoRepository
            .findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
                employeeId,
                deviceId,
                BiometricType.valueOf(biometricType)
            )
            .orElseThrow(() -> new BaseException(BaseResponseStatus.BIOMETRIC_NOT_FOUND));

        biometricInfo.setIsActive(false);
        biometricInfoRepository.save(biometricInfo);

        // 실패 횟수도 초기화
        biometricFailureService.clearFailCount(employeeId, deviceId);

        log.info("생체인증 해제 완료: id={}", biometricInfo.getId());

        return BiometricResponseDto.builder()
            .success(true)
            .message("생체인증이 해제되었습니다.")
            .build();
    }
}
