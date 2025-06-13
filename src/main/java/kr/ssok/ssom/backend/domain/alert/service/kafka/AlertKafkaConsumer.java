package kr.ssok.ssom.backend.domain.alert.service.kafka;

import kr.ssok.ssom.backend.domain.alert.dto.AlertResponseDto;
import kr.ssok.ssom.backend.domain.alert.dto.kafka.AlertCreatedEvent;
import kr.ssok.ssom.backend.domain.alert.dto.kafka.UserAlertEvent;
import kr.ssok.ssom.backend.domain.alert.entity.Alert;
import kr.ssok.ssom.backend.domain.alert.entity.AlertStatus;
import kr.ssok.ssom.backend.domain.alert.repository.AlertRepository;
import kr.ssok.ssom.backend.domain.alert.repository.AlertStatusRepository;
import kr.ssok.ssom.backend.domain.alert.service.AlertService;
import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Kafka Consumer 서비스
 * Alert 관련 이벤트를 소비하여 비동기 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertKafkaConsumer {
    
    private final AlertRepository alertRepository;
    private final AlertStatusRepository alertStatusRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;
    private final AlertKafkaProducer alertKafkaProducer;

    /**
     * Alert 생성 이벤트 소비
     * Alert가 생성된 후 대상 사용자들에게 개별 알림 이벤트 발행
     */
    @KafkaListener(topics = "#{@kafkaConfig.alertCreatedTopic().name()}", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleAlertCreated(
            AlertCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.info("[Kafka Consumer] Alert 생성 이벤트 수신 - alertId: {}, topic: {}, partition: {}, offset: {}", 
                event.getAlertId(), topic, partition, offset);
        
        try {
            // Alert 조회
            Alert alert = alertRepository.findById(event.getAlertId())
                    .orElseThrow(() -> new RuntimeException("Alert not found: " + event.getAlertId()));
            
            // 대상 사용자 필터링
            List<User> targetUsers = getTargetUsers(event.getAppName(), event.getAlertKind());
            
            log.info("[Kafka Consumer] 대상 사용자 {}명에게 알림 이벤트 발행 - alertId: {}", 
                    targetUsers.size(), event.getAlertId());
            
            // 각 사용자에게 개별 알림 이벤트 발행
            for (User user : targetUsers) {
                UserAlertEvent userEvent = UserAlertEvent.of(event.getAlertId(), user.getId());
                alertKafkaProducer.publishUserAlert(userEvent);
            }
            
            // 성공적으로 처리된 경우에만 오프셋 커밋
            acknowledgment.acknowledge();
            
            log.info("[Kafka Consumer] Alert 생성 이벤트 처리 완료 - alertId: {}, targetUsers: {}", 
                    event.getAlertId(), targetUsers.size());
            
        } catch (Exception e) {
            log.error("[Kafka Consumer] Alert 생성 이벤트 처리 실패 - alertId: {}, error: {}", 
                    event.getAlertId(), e.getMessage(), e);
            
            // 오프셋 커밋하지 않음 (재처리됨)
            throw new RuntimeException("Failed to process alert created event", e);
        }
    }

    /**
     * 사용자별 알림 이벤트 소비
     * 개별 사용자에게 AlertStatus 생성 및 SSE/FCM 전송 (병렬 처리)
     */
    @KafkaListener(topics = "#{@kafkaConfig.userAlertTopic().name()}", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleUserAlert(
            @Payload UserAlertEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.debug("[Kafka Consumer] 사용자 알림 이벤트 수신 - alertId: {}, userId: {}, topic: {}, partition: {}", 
                event.getAlertId(), event.getUserId(), topic, partition);
        
        try {
            // Alert 조회
            Alert alert = alertRepository.findById(event.getAlertId())
                    .orElseThrow(() -> new RuntimeException("Alert not found: " + event.getAlertId()));
            
            // User 조회
            User user = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + event.getUserId()));
            
            // 중복 처리 방지 (멱등성 보장)
            if (alertStatusRepository.existsByAlert_AlertIdAndUser_Id(event.getAlertId(), event.getUserId())) {
                log.info("[Kafka Consumer] 이미 처리된 알림 - alertId: {}, userId: {}", 
                        event.getAlertId(), event.getUserId());
                acknowledgment.acknowledge();
                return;
            }
            
            // AlertStatus 생성
            AlertStatus alertStatus = AlertStatus.builder()
                    .alert(alert)
                    .user(user)
                    .isRead(false)
                    .build();
            alertStatusRepository.save(alertStatus);
            
            // SSE/FCM 전송
            AlertResponseDto responseDto = AlertResponseDto.from(alertStatus);
            alertService.sendAlertToUser(event.getUserId(), responseDto);
            
            // 성공적으로 처리된 경우에만 오프셋 커밋
            acknowledgment.acknowledge();
            
            log.debug("[Kafka Consumer] 사용자 알림 처리 완료 - alertId: {}, userId: {}", 
                    event.getAlertId(), event.getUserId());
            
        } catch (Exception e) {
            log.error("[Kafka Consumer] 사용자 알림 처리 실패 - alertId: {}, userId: {}, error: {}", 
                    event.getAlertId(), event.getUserId(), e.getMessage(), e);
            
            // 오프셋 커밋하지 않음 (재처리됨)
            throw new RuntimeException("Failed to process user alert event", e);
        }
    }

    /**
     * 알림 대상 사용자 필터링
     * 기존 로직을 재사용
     */
    private List<User> getTargetUsers(String appName, kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind alertKind) {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .filter(user -> {
                    Department dept = user.getDepartment();
                    String lowerApp = appName != null ? appName.toLowerCase() : "";

                    if (dept == Department.OPERATION || dept == Department.EXTERNAL) return true;
                    if (dept == Department.CORE_BANK) return lowerApp.contains("ssok-bank");
                    if (dept == Department.CHANNEL) return !lowerApp.contains("ssok-bank");

                    return false;
                })
                .collect(Collectors.toList());
    }
}
