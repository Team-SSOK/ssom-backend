package kr.ssok.ssom.backend.domain.alert.entity;


import jakarta.persistence.*;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlertStatus> alertStatuses = new ArrayList<>();

    //제목
    private String title;

    //메세지
    private String message;

    //종류
    @Enumerated(EnumType.STRING)
    private AlertKind kind;

    //발생 시간
    private String timestamp;

    //발송 시간
    @CreationTimestamp
    private LocalDateTime createdAt;
}
