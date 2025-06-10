package kr.ssok.ssom.backend.domain.alert.repository;

import kr.ssok.ssom.backend.domain.alert.entity.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public interface AlertStatusRepository extends JpaRepository<AlertStatus, Long> {

    List<AlertStatus> findByUser_IdAndAlert_CreatedAtAfterOrderByAlert_TimestampDesc(String employeeId, LocalDateTime timestamp);;

    List<AlertStatus> findByUser_IdAndIsReadFalse(String employeeId);
    List<AlertStatus> findByUser_IdOrderByAlert_CreatedAtDesc(String employeeId);

}
