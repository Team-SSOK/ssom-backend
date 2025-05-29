package kr.ssok.ssom.backend.domain.user.repository;

import kr.ssok.ssom.backend.domain.user.entity.BiometricInfo;
import kr.ssok.ssom.backend.domain.user.entity.BiometricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricInfoRepository extends JpaRepository<BiometricInfo, Long> {

    /**
     * 사용자의 활성화된 생체인증 정보 조회
     */
    List<BiometricInfo> findByEmployeeIdAndIsActiveTrue(String employeeId);

    /**
     * 특정 디바이스의 생체인증 정보 조회
     */
    Optional<BiometricInfo> findByEmployeeIdAndDeviceIdAndIsActiveTrue(
            String employeeId, String deviceId);

    /**
     * 특정 사용자, 디바이스, 타입의 생체인증 정보 조회 (로그인 시 사용)
     */
    Optional<BiometricInfo> findByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
            String employeeId, String deviceId, BiometricType biometricType);

    /**
     * 특정 사용자, 디바이스, 타입의 생체인증 존재 여부 확인 (등록 시 중복 체크)
     */
    boolean existsByEmployeeIdAndDeviceIdAndBiometricTypeAndIsActiveTrue(
            String employeeId, String deviceId, BiometricType biometricType);

    /**
     * 특정 사용자, 디바이스의 생체인증 존재 여부 확인
     */
    boolean existsByEmployeeIdAndDeviceIdAndIsActiveTrue(
            String employeeId, String deviceId);

    /**
     * 사용자의 활성화된 생체인증 정보 개수 조회
     */
    int countByEmployeeIdAndIsActiveTrue(String employeeId);

    /**
     * 사용자별 등록된 디바이스 수 조회
     */
    @Query("SELECT COUNT(b) FROM BiometricInfo b WHERE b.employeeId = :employeeId AND b.isActive = true")
    int countActiveDevicesByEmployeeId(@Param("employeeId") String employeeId);

    /**
     * 특정 타입의 생체인증 정보만 조회
     */
    List<BiometricInfo> findByEmployeeIdAndBiometricTypeAndIsActiveTrue(
            String employeeId, BiometricType biometricType);

    /**
     * 특정 기간 동안 사용되지 않은 생체인증 정보 조회 (정리용)
     */
    @Query("SELECT b FROM BiometricInfo b WHERE b.lastUsedAt < :cutoffDate AND b.isActive = true")
    List<BiometricInfo> findUnusedBiometricInfo(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 마지막 사용 시간 업데이트
     */
    @Modifying
    @Query("UPDATE BiometricInfo b SET b.lastUsedAt = :lastUsedAt WHERE b.id = :id")
    int updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") LocalDateTime lastUsedAt);

    /**
     * 디바이스별 생체인증 비활성화
     */
    @Modifying
    @Query("UPDATE BiometricInfo b SET b.isActive = false WHERE b.employeeId = :employeeId AND b.deviceId = :deviceId")
    int deactivateByEmployeeIdAndDeviceId(@Param("employeeId") String employeeId, @Param("deviceId") String deviceId);

    /**
     * 특정 타입의 생체인증만 비활성화
     */
    @Modifying
    @Query("UPDATE BiometricInfo b SET b.isActive = false WHERE b.employeeId = :employeeId AND b.deviceId = :deviceId AND b.biometricType = :biometricType")
    int deactivateByEmployeeIdAndDeviceIdAndBiometricType(
            @Param("employeeId") String employeeId, 
            @Param("deviceId") String deviceId, 
            @Param("biometricType") BiometricType biometricType);

    /**
     * 사용자의 모든 생체인증 비활성화
     */
    @Modifying
    @Query("UPDATE BiometricInfo b SET b.isActive = false WHERE b.employeeId = :employeeId")
    int deactivateAllByEmployeeId(@Param("employeeId") String employeeId);
}