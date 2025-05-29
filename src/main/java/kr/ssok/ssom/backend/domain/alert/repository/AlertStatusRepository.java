package kr.ssok.ssom.backend.domain.alert.repository;

import kr.ssok.ssom.backend.domain.alert.entity.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertStatusRepository extends JpaRepository<AlertStatus, Long> {

    //List<AlertStatus> findByUserIdAndAlertKind(Long userId, AlertKind kind);

    List<AlertStatus> findByUsername(String username);
}
