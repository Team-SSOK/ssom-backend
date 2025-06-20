package kr.ssok.ssom.backend.domain.alert.repository;

import kr.ssok.ssom.backend.domain.alert.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    // 테스트용 존재 여부 확인 메서드 (Alert 엔티티의 id 필드로 검색)
    boolean existsById(String id);
    
    // Alert 엔티티의 id 필드로 검색
    Alert findById(String id);
    
    // 성능 테스트를 위한 추가 메서드들
    List<Alert> findByIdContaining(String keyword);

    Optional<Alert> findByAlertId(Long alertId);
}

