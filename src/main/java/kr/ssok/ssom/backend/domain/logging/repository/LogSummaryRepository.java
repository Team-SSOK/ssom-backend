package kr.ssok.ssom.backend.domain.logging.repository;

import kr.ssok.ssom.backend.domain.logging.entity.LogSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LogSummaryRepository extends JpaRepository<LogSummary, String> {
    Optional<LogSummary> findByLogId(String logId);
}
