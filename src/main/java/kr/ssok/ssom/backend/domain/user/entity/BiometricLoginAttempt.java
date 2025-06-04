package kr.ssok.ssom.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_login_attempts")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BiometricLoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", length = 10)
    private String employeeId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "biometric_type", nullable = false)
    private BiometricType biometricType;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_result", nullable = false)
    private AttemptResult attemptResult;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "attempted_at", nullable = false)
    @Builder.Default
    private LocalDateTime attemptedAt = LocalDateTime.now();

    // 인덱스 추가
    @Table(indexes = {
        @Index(name = "idx_employee_attempts", columnList = "employee_id, attempted_at"),
        @Index(name = "idx_device_attempts", columnList = "device_id, attempted_at")
    })
    public static class Indexes {}
}