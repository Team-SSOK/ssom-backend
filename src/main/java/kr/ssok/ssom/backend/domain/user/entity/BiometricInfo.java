package kr.ssok.ssom.backend.domain.user.entity;

import jakarta.persistence.*;
import kr.ssok.ssom.backend.global.entity.TimeStamp;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_info", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_biometric_employee_device_type",
        columnNames = {"employee_id", "device_id", "biometric_type"}
    )
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BiometricInfo extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, length = 10)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "biometric_type", nullable = false)
    private BiometricType biometricType;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "biometric_hash", nullable = false, length = 500)
    private String biometricHash;

    @Column(name = "device_info", columnDefinition = "JSON")
    private String deviceInfo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // User 엔티티와의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id", insertable = false, updatable = false)
    private User user;
}