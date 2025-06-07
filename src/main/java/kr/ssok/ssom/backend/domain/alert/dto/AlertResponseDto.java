package kr.ssok.ssom.backend.domain.alert.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ssok.ssom.backend.domain.alert.entity.Alert;
import kr.ssok.ssom.backend.domain.alert.entity.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Tag(name = "AlertResponseDto", description = "알림 전송을 위한 공통 포맷")
@Getter
@Builder
@AllArgsConstructor
public class AlertResponseDto {
    private Long alertId;
    private Long alertStatusId;
    private String id;
    private String title;
    private String message;
    private String kind;

    @JsonProperty("isRead")
    private boolean isRead;

    private String timestamp;
    private LocalDateTime createdAt;
    private String employeeId;

    public static AlertResponseDto from(AlertStatus status) {
        Alert alert = status.getAlert();
        return AlertResponseDto.builder()
                .alertId(alert.getAlertId())                // 예 : 1
                .alertStatusId(status.getAlertStatusId())   // 예 : 2
                .id(alert.getId())                      // 예 : "686692198126160f"
                .title(alert.getTitle())                // 예 : [ERROR] ssok-bank
                .message(alert.getMessage())            // 예 : "Authentication error: Authorization header is missing or invalid"
                .kind(alert.getKind().getValue())       // 예 : "OPENSEARCH"
                .isRead(status.isRead())                // 예 : false
                .timestamp(alert.getTimestamp())        // 예 : "2025-05-30T07:24:06.396205638+00:00" -> 발생시간
                .createdAt(alert.getCreatedAt())        // 예 : "2025-05-30T07:24:06.396205638+00:00" -> alert 저장시간
                .employeeId(status.getUser().getId())   // 예 : (사원번호)
                .build();
    }
}
