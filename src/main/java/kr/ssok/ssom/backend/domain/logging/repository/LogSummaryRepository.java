package kr.ssok.ssom.backend.domain.logging.repository;

import kr.ssok.ssom.backend.domain.logging.entity.LogSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogSummaryRepository extends JpaRepository<LogSummary, String> {
}
