package kr.ssok.ssom.backend.domain.alert.repository;

import kr.ssok.ssom.backend.domain.alert.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    // 테스트용 존재 여부 확인 메서드 (Alert 엔티티의 id 필드로 검색)
    boolean existsById(String id);
    
    // Alert 엔티티의 id 필드로 검색
    Alert findById(String id);
}
