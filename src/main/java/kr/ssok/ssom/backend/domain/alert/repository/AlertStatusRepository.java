package kr.ssok.ssom.backend.domain.alert.repository;

import kr.ssok.ssom.backend.domain.alert.entity.AlertStatus;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertStatusRepository extends JpaRepository<AlertStatus, Long> {

    //List<AlertStatus> findByUserIdAndAlertKind(Long userId, AlertKind kind);

    List<AlertStatus> findByUserId(Long userId);
}
