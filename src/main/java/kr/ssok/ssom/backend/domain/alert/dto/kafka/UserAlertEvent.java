package kr.ssok.ssom.backend.domain.alert.dto.kafka;

import lombok.*;

/**
 * Kafka용 사용자별 알림 이벤트 DTO
 * 각 사용자에게 개별적으로 알림을 전송하기 위한 이벤트
 */
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class UserAlertEvent {
    
    private Long alertId;
    private String userId;
    
    /**
     * 빌더 패턴으로 생성
     */
    public static UserAlertEvent of(Long alertId, String userId) {
        return UserAlertEvent.builder()
                .alertId(alertId)
                .userId(userId)
                .build();
    }
}
