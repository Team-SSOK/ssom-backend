package kr.ssok.ssom.backend.domain.logging.repository;

import kr.ssok.ssom.backend.domain.logging.entity.Logging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoggingRepository extends JpaRepository<Logging, Long> {
    
    /**
     * 로그 ID로 로그 조회
     * @param logId 로그 ID
     * @return 로그 엔티티
     */
    Optional<Logging> findByLogId(String logId);
    
    /**
     * 여러 로그 ID로 로그 목록 조회
     * @param logIds 로그 ID 목록
     * @return 로그 엔티티 목록
     */
    @Query("SELECT l FROM Logging l WHERE l.logId IN :logIds")
    List<Logging> findByLogIds(@Param("logIds") List<String> logIds);
    
    /**
     * 특정 애플리케이션의 로그 조회
     * @param app 애플리케이션명
     * @return 로그 엔티티 목록
     */
    List<Logging> findByApp(String app);
    
    /**
     * 특정 로그 레벨의 로그 조회
     * @param level 로그 레벨
     * @return 로그 엔티티 목록
     */
    List<Logging> findByLevel(String level);
}
