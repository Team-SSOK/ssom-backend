package kr.ssok.ssom.backend.domain.alert.dto.kafka;

import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import lombok.*;

/**
 * Kafka용 Alert 생성 이벤트 DTO
 * Alert가 생성된 후 AlertStatus 생성을 위한 이벤트
 */
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AlertCreatedEvent {
    
    private Long alertId;
    private AlertKind alertKind;
    private String appName;
    
    /**
     * 빌더 패턴으로 생성
     */
    public static AlertCreatedEvent of(Long alertId, AlertKind alertKind, String appName) {
        return AlertCreatedEvent.builder()
                .alertId(alertId)
                .alertKind(alertKind)
                .appName(appName)
                .build();
    }
}
