package kr.ssok.ssom.backend.domain.user.repository;

import kr.ssok.ssom.backend.domain.user.entity.BiometricLoginAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BiometricLoginAttemptRepository extends JpaRepository<BiometricLoginAttempt, Long> {

    /**
     * 특정 기간 내 디바이스별 실패 시도 횟수 조회
     */
    @Query("SELECT COUNT(b) FROM BiometricLoginAttempt b " +
           "WHERE b.deviceId = :deviceId " +
           "AND b.attemptResult = 'FAILED' " +
           "AND b.attemptedAt >= :since")
    int countFailedAttemptsSince(@Param("deviceId") String deviceId, @Param("since") LocalDateTime since);

    /**
     * 사용자별 최근 로그인 시도 조회
     */
    List<BiometricLoginAttempt> findByEmployeeIdOrderByAttemptedAtDesc(String employeeId, Pageable pageable);

    /**
     * 디바이스별 최근 로그인 시도 조회
     */
    List<BiometricLoginAttempt> findByDeviceIdOrderByAttemptedAtDesc(String deviceId, Pageable pageable);
}