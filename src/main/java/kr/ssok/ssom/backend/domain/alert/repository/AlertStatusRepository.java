package kr.ssok.ssom.backend.domain.alert.repository;

import kr.ssok.ssom.backend.domain.alert.entity.Alert;
import kr.ssok.ssom.backend.domain.alert.entity.AlertStatus;
import kr.ssok.ssom.backend.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public interface AlertStatusRepository extends JpaRepository<AlertStatus, Long> {
    // 목록 조회
    List<AlertStatus> findByUser_IdAndAlert_CreatedAtAfterOrderByAlert_TimestampDesc(String employeeId, LocalDateTime from);
    Page<AlertStatus> findByUser_IdAndAlert_CreatedAtAfter(String employeeId, LocalDateTime after, Pageable pageable);
    // 상태 변경 전 조회
    List<AlertStatus> findByUser_IdAndIsReadFalse(String employeeId);
    List<AlertStatus> findByUser_IdOrderByAlert_CreatedAtDesc(String employeeId);
    
    // 테스트용 카운트 메서드
    long countByAlert_Id(String alertId);
    
    // Alert 엔티티의 id 필드로 검색하는 메서드 (String 타입)
    boolean existsByAlert_Id(String alertId);

    // Kafka 중복 처리 방지를 위한 메서드 (Alert와 User 엔티티 기반)
    boolean existsByAlertAndUser(Alert alert, User user);
    
    // AlertId와 UserId로 중복 체크 (성능 최적화)
    boolean existsByAlert_IdAndUser_Id(Long alertId, String userId);
}
