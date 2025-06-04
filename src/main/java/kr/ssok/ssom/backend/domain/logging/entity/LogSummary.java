package kr.ssok.ssom.backend.domain.logging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSummary {
    @Id
    private String logId;
    private String summary;
    private String fileLocation;
    private String functionLocation;
    private String solution;
    @Lob
    private String solutionDetail;
}
