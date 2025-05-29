package kr.ssok.ssom.backend.domain.user.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 생체인증 관련 설정값들을 관리하는 Configuration 클래스
 */
@Configuration
@ConfigurationProperties(prefix = "biometric")
@Getter
public class BiometricConfig {
    
    /**
     * 생체인증 최대 실패 허용 횟수 (기본값: 3회)
     */
    private int maxFailureAttempts = 3;
    
    /**
     * 실패 횟수 초기화 시간 (분 단위, 기본값: 30분)
     */
    private int failureResetTimeMinutes = 30;
    
    /**
     * 디바이스 차단 시간 (분 단위, 기본값: 30분)
     */
    private int deviceBlockTimeMinutes = 30;
    
    /**
     * 타임스탬프 허용 오차 범위 (밀리초 단위, 기본값: 5분)
     */
    private long timestampToleranceMs = 300000; // 5분
    
    /**
     * 생체인증 해시 최대 길이
     */
    private int maxBiometricHashLength = 500;
    
    /**
     * 디바이스 정보 최대 길이
     */
    private int maxDeviceInfoLength = 1000;
    
    // Setter methods for Spring Boot Configuration Properties
    public void setMaxFailureAttempts(int maxFailureAttempts) {
        this.maxFailureAttempts = maxFailureAttempts;
    }
    
    public void setFailureResetTimeMinutes(int failureResetTimeMinutes) {
        this.failureResetTimeMinutes = failureResetTimeMinutes;
    }
    
    public void setDeviceBlockTimeMinutes(int deviceBlockTimeMinutes) {
        this.deviceBlockTimeMinutes = deviceBlockTimeMinutes;
    }
    
    public void setTimestampToleranceMs(long timestampToleranceMs) {
        this.timestampToleranceMs = timestampToleranceMs;
    }
    
    public void setMaxBiometricHashLength(int maxBiometricHashLength) {
        this.maxBiometricHashLength = maxBiometricHashLength;
    }
    
    public void setMaxDeviceInfoLength(int maxDeviceInfoLength) {
        this.maxDeviceInfoLength = maxDeviceInfoLength;
    }
}
