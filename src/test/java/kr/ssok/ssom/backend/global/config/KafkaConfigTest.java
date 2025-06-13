package kr.ssok.ssom.backend.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka TopicBuilder를 통한 토픽 자동 생성 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Kafka Topic 자동 생성 테스트")
class KafkaConfigTest {

    @Autowired
    private KafkaConfig kafkaConfig;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Test
    @DisplayName("Alert Created Topic Bean이 올바르게 생성되는지 확인")
    void testAlertCreatedTopicBean() {
        // When
        NewTopic alertCreatedTopic = kafkaConfig.alertCreatedTopic();
        
        // Then
        assertThat(alertCreatedTopic).isNotNull();
        assertThat(alertCreatedTopic.name()).isEqualTo("alert-created-topic");
        assertThat(alertCreatedTopic.numPartitions()).isEqualTo(1); // test 프로파일에서 1개
        assertThat(alertCreatedTopic.replicationFactor()).isEqualTo((short) 1);
        
        // 토픽 설정 확인
        assertThat(alertCreatedTopic.configs()).containsEntry("compression.type", "lz4");
        assertThat(alertCreatedTopic.configs()).containsEntry("cleanup.policy", "delete");
    }

    @Test
    @DisplayName("User Alert Topic Bean이 올바르게 생성되는지 확인")
    void testUserAlertTopicBean() {
        // When
        NewTopic userAlertTopic = kafkaConfig.userAlertTopic();
        
        // Then
        assertThat(userAlertTopic).isNotNull();
        assertThat(userAlertTopic.name()).isEqualTo("user-alert-topic");
        assertThat(userAlertTopic.numPartitions()).isEqualTo(1); // test 프로파일에서 1개
        assertThat(userAlertTopic.replicationFactor()).isEqualTo((short) 1);
        
        // 토픽 설정 확인
        assertThat(userAlertTopic.configs()).containsEntry("compression.type", "lz4");
        assertThat(userAlertTopic.configs()).containsEntry("cleanup.policy", "delete");
    }

    @Test
    @DisplayName("Dead Letter Queue Topic Bean이 올바르게 생성되는지 확인")
    void testDeadLetterTopicBean() {
        // When
        NewTopic deadLetterTopic = kafkaConfig.deadLetterTopic();
        
        // Then
        assertThat(deadLetterTopic).isNotNull();
        assertThat(deadLetterTopic.name()).isEqualTo("alert-dlq-topic");
        assertThat(deadLetterTopic.numPartitions()).isEqualTo(1); // DLQ는 항상 1개
        assertThat(deadLetterTopic.replicationFactor()).isEqualTo((short) 1);
        
        // 토픽 설정 확인 (더 긴 보관 기간)
        assertThat(deadLetterTopic.configs()).containsEntry("retention.ms", "2592000000"); // 30일
    }

    @Test
    @DisplayName("KafkaAdmin Bean이 올바르게 구성되는지 확인")
    void testKafkaAdminBean() {
        // Then
        assertThat(kafkaAdmin).isNotNull();
        assertThat(kafkaAdmin.getConfigurationProperties())
                .containsKey("bootstrap.servers");
    }

    @Test
    @DisplayName("환경별 토픽 설정이 올바르게 적용되는지 확인")
    void testTopicConfigurationByProfile() {
        // Given - test 프로파일 활성화
        NewTopic alertTopic = kafkaConfig.alertCreatedTopic();
        NewTopic userTopic = kafkaConfig.userAlertTopic();
        
        // Then - test 프로파일 설정 확인 (파티션 1개, 복제 1개)
        assertThat(alertTopic.numPartitions()).isEqualTo(1);
        assertThat(userTopic.numPartitions()).isEqualTo(1);
        assertThat(alertTopic.replicationFactor()).isEqualTo((short) 1);
        assertThat(userTopic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("토픽 이름이 설정값과 일치하는지 확인")
    void testTopicNames() {
        // When
        NewTopic alertTopic = kafkaConfig.alertCreatedTopic();
        NewTopic userTopic = kafkaConfig.userAlertTopic();
        NewTopic dlqTopic = kafkaConfig.deadLetterTopic();
        
        // Then
        assertThat(alertTopic.name()).matches("alert-created-topic");
        assertThat(userTopic.name()).matches("user-alert-topic");
        assertThat(dlqTopic.name()).matches("alert-dlq-topic");
    }

    @Test
    @DisplayName("토픽 설정값들이 올바르게 적용되는지 확인")
    void testTopicConfigurations() {
        // When
        NewTopic alertTopic = kafkaConfig.alertCreatedTopic();
        NewTopic userTopic = kafkaConfig.userAlertTopic();
        
        // Then - 공통 설정
        assertThat(alertTopic.configs())
                .containsEntry("compression.type", "lz4")
                .containsEntry("cleanup.policy", "delete")
                .containsKey("retention.ms")
                .containsKey("min.insync.replicas");
        
        assertThat(userTopic.configs())
                .containsEntry("compression.type", "lz4")
                .containsEntry("cleanup.policy", "delete")
                .containsKey("retention.ms")
                .containsKey("min.insync.replicas");
        
        // User Alert Topic 전용 설정
        assertThat(userTopic.configs()).containsKey("max.message.bytes");
    }
}
