package kr.ssok.ssom.backend.domain.alert.service.kafka;

import kr.ssok.ssom.backend.domain.alert.dto.kafka.AlertCreatedEvent;
import kr.ssok.ssom.backend.domain.alert.dto.kafka.UserAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer 서비스
 * Alert 관련 이벤트를 Kafka로 발행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertKafkaProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // 토픽 이름을 설정에서 주입받음
    @Value("${alert.kafka.topics.alert-created}")
    private String alertCreatedTopic;
    
    @Value("${alert.kafka.topics.user-alert}")
    private String userAlertTopic;

    /**
     * Alert 생성 이벤트 발행
     * Alert가 생성된 후 AlertStatus 생성을 위한 이벤트
     */
    public void publishAlertCreated(AlertCreatedEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(alertCreatedTopic, String.valueOf(event.getAlertId()), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[Kafka] Alert 생성 이벤트 발행 성공 - alertId: {}, topic: {}, partition: {}, offset: {}", 
                            event.getAlertId(), alertCreatedTopic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("[Kafka] Alert 생성 이벤트 발행 실패 - alertId: {}, topic: {}, error: {}", 
                            event.getAlertId(), alertCreatedTopic, ex.getMessage(), ex);
                }
            });
            
        } catch (Exception e) {
            log.error("[Kafka] Alert 생성 이벤트 발행 중 예외 - alertId: {}, topic: {}, error: {}", 
                    event.getAlertId(), alertCreatedTopic, e.getMessage(), e);
            throw new RuntimeException("Failed to publish alert created event", e);
        }
    }

    /**
     * 사용자별 알림 이벤트 발행
     * 개별 사용자에게 알림을 전송하기 위한 이벤트
     */
    public void publishUserAlert(UserAlertEvent event) {
        try {
            // 사용자별로 파티셔닝하여 순서 보장
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(userAlertTopic, event.getUserId(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("[Kafka] 사용자 알림 이벤트 발행 성공 - alertId: {}, userId: {}, topic: {}, partition: {}", 
                            event.getAlertId(), event.getUserId(), userAlertTopic, result.getRecordMetadata().partition());
                } else {
                    log.error("[Kafka] 사용자 알림 이벤트 발행 실패 - alertId: {}, userId: {}, topic: {}, error: {}", 
                            event.getAlertId(), event.getUserId(), userAlertTopic, ex.getMessage(), ex);
                }
            });
            
        } catch (Exception e) {
            log.error("[Kafka] 사용자 알림 이벤트 발행 중 예외 - alertId: {}, userId: {}, topic: {}, error: {}", 
                    event.getAlertId(), event.getUserId(), userAlertTopic, e.getMessage(), e);
            throw new RuntimeException("Failed to publish user alert event", e);
        }
    }
}
