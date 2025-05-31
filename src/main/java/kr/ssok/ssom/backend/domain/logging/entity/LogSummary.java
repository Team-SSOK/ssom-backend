package kr.ssok.ssom.backend.domain.logging.entity;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

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
}
