package kr.ssok.ssom.backend.domain.logging.entity;

import jakarta.persistence.*;
import kr.ssok.ssom.backend.global.entity.TimeStamp;
import lombok.*;

/**
 * 로그 정보를 저장하는 엔티티
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "logs")
public class Logging extends TimeStamp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_table_id")
    private Long id;
    
    @Column(name = "log_id", unique = true, nullable = false, length = 100)
    private String logId;           // 로그 고유 ID
    
    @Column(name = "level", length = 10)
    private String level;           // 로그 레벨 (ERROR, WARN, INFO, DEBUG)
    
    @Column(name = "logger", length = 500)
    private String logger;          // 로거 클래스명
    
    @Column(name = "thread", length = 100)
    private String thread;          // 스레드명
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;         // 로그 메시지
    
    @Column(name = "app", length = 100)
    private String app;             // 애플리케이션명
    
    @Column(name = "timestamp")
    private String timestamp;       // 로그 발생 시간
    
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;      // 스택 트레이스 (선택사항)
}
