package kr.ssok.ssom.backend.domain.alert.entity;

import jakarta.persistence.*;
import kr.ssok.ssom.backend.domain.user.entity.User;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AlertStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    //읽음 여부
    private boolean isRead;

    //수정 일자
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsUnread() {
        this.isRead = false;
    }
}
