package kr.ssok.ssom.backend.domain.alert.dto;

import kr.ssok.ssom.backend.domain.alert.entity.Alert;
import kr.ssok.ssom.backend.domain.alert.entity.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AlertResponseDto {
    private Long alertId;
    private String title;
    private String message;
    private String kind;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String employeeId;

    public static AlertResponseDto from(AlertStatus status) {
        Alert alert = status.getAlert();
        return AlertResponseDto.builder()
                .alertId(alert.getAlertId())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .kind(alert.getKind().getValue())
                .isRead(status.isRead())
                .createdAt(alert.getCreatedAt())
                .employeeId(status.getUser().getId())
                .build();
    }
}
