package kr.ssok.ssom.backend.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:ssom-alert-consumer}")
    private String groupId;

    // 토픽 설정값들
    @Value("${alert.kafka.topics.alert-created:alert-created-topic}")
    private String alertCreatedTopic;

    @Value("${alert.kafka.topics.user-alert:user-alert-topic}")
    private String userAlertTopic;

    @Value("${alert.kafka.topic-config.alert-created.partitions:3}")
    private int alertCreatedPartitions;

    @Value("${alert.kafka.topic-config.user-alert.partitions:10}")
    private int userAlertPartitions;

    @Value("${alert.kafka.topic-config.replication-factor:1}")
    private int replicationFactor;

    @Value("${alert.kafka.topic-config.retention-ms:604800000}") // 7일
    private String retentionMs;

    // ===========================
    // Topic 자동 생성 Bean들
    // ===========================

    /**
     * Alert 생성 이벤트 토픽
     * - Alert가 생성된 후 대상 사용자 필터링을 위한 토픽
     * - 파티션 수가 적어도 됨 (처리량이 상대적으로 낮음)
     */
    @Bean
    public NewTopic alertCreatedTopic() {
        log.info("Alert Created Topic 설정 - 토픽명: {}, 파티션: {}, 복제: {}",
                alertCreatedTopic, alertCreatedPartitions, replicationFactor);
        
        return TopicBuilder.name(alertCreatedTopic)
                .partitions(alertCreatedPartitions)
                .replicas(replicationFactor)
                .config("retention.ms", retentionMs)
                .config("compression.type", "lz4")
                .config("cleanup.policy", "delete")
                .config("min.insync.replicas", String.valueOf(Math.max(1, replicationFactor - 1)))
                .build();
    }

    /**
     * 사용자별 알림 토픽
     * - 개별 사용자에게 알림을 전송하기 위한 토픽
     * - 파티션 수가 많아야 함 (높은 처리량, 병렬 처리)
     */
    @Bean
    public NewTopic userAlertTopic() {
        log.info("User Alert Topic 설정 - 토픽명: {}, 파티션: {}, 복제: {}",
                userAlertTopic, userAlertPartitions, replicationFactor);
        
        return TopicBuilder.name(userAlertTopic)
                .partitions(userAlertPartitions)
                .replicas(replicationFactor)
                .config("retention.ms", retentionMs)
                .config("compression.type", "lz4")
                .config("cleanup.policy", "delete")
                .config("min.insync.replicas", String.valueOf(Math.max(1, replicationFactor - 1)))
                // 사용자별 순서 보장을 위한 설정
                .config("max.message.bytes", "1048576") // 1MB
                .build();
    }

    /**
     * Dead Letter Queue 토픽 (실패한 메시지 처리용)
     * - 처리 실패한 메시지들을 별도로 저장
     * - 수동 재처리나 디버깅용으로 사용
     */
    @Bean
    public NewTopic deadLetterTopic() {
        String dlqTopic = "alert-dlq-topic";
        log.info("Dead Letter Queue Topic 설정 - 토픽명: {}", dlqTopic);
        
        return TopicBuilder.name(dlqTopic)
                .partitions(1) // DLQ는 파티션 1개로 충분
                .replicas(replicationFactor)
                .config("retention.ms", "2592000000") // 30일 (더 길게 보관)
                .config("compression.type", "lz4")
                .config("cleanup.policy", "delete")
                .build();
    }

    // ===========================
    // Producer/Consumer 설정  
    // ===========================

    /**
     * Kafka Producer 설정
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // 성능 최적화 설정
        configProps.put(ProducerConfig.ACKS_CONFIG, "1"); // 리더만 확인
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5); // 5ms 대기 후 배치 전송
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        
        // 추가 최적화 설정
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        
        // 기본 토픽 설정 (선택사항)
        template.setDefaultTopic(alertCreatedTopic);
        
        return template;
    }

    /**
     * Kafka Consumer 설정
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        
        // JSON 역직렬화 설정 강화
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "kr.ssok.ssom.backend.domain.alert.dto.kafka.AlertCreatedEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);
        
        // Consumer 최적화 설정
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 커밋
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10); // 한번에 처리할 레코드 수
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(10); // 동시 처리 스레드 수 (병렬 처리)
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // 에러 핸들링 설정 - Spring Kafka 2.8+ 방식
        factory.setCommonErrorHandler(new DefaultErrorHandler((consumerRecord, exception) -> {
            log.error("Kafka Consumer 에러 발생 - Topic: {}, Partition: {}, Offset: {}, Key: {}, Value: {}, Error: {}", 
                    consumerRecord.topic(), 
                    consumerRecord.partition(), 
                    consumerRecord.offset(),
                    consumerRecord.key(), 
                    consumerRecord.value(), 
                    exception.getMessage(), exception);
        }, new FixedBackOff(1000L, 3L))); // 1초 간격으로 3번 재시도
        
        return factory;
    }

    /**
     * Kafka Admin 설정 (토픽 생성을 위해 필요)
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setFatalIfBrokerNotAvailable(false); // 브로커가 없어도 애플리케이션 시작 허용
        
        log.info("🔧 Kafka Admin 설정 완료 - Bootstrap Servers: {}", bootstrapServers);
        return admin;
    }
}
