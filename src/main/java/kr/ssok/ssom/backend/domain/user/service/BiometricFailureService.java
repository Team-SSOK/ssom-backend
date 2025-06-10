package kr.ssok.ssom.backend.domain.user.service;

public interface BiometricFailureService {

    /**
     * 실패 횟수 증가
     */
    int incrementFailCount(String employeeId, String deviceId);

    /**
     * 실패 횟수 초기화
     */
    void clearFailCount(String employeeId, String deviceId);

    /**
     * 현재 실패 횟수 조회
     */
    int getFailCount(String employeeId, String deviceId);

    /**
     * 최대 시도 횟수 초과 여부 확인
     */
    boolean hasExceededMaxAttempts(String employeeId, String deviceId);

    /**
     * 디바이스 차단
     */
    void blockDevice(String employeeId, String deviceId);

    /**
     * 디바이스 차단 상태 확인
     */
    boolean isDeviceBlocked(String employeeId, String deviceId);

    /**
     * 디바이스 차단 해제
     */
    void unblockDevice(String employeeId, String deviceId);

    /**
     * 남은 시도 횟수 반환
     */
    int getRemainingAttempts(String employeeId, String deviceId);

    /**
     * 사용자의 모든 디바이스 차단 해제 (일반 로그인 성공 시 사용)
     */
    void unblockAllDevicesForUser(String employeeId);
}
