package kr.ssok.ssom.backend.domain.alert.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ssok.ssom.backend.domain.alert.dto.*;
import kr.ssok.ssom.backend.domain.alert.entity.Alert;
import kr.ssok.ssom.backend.domain.alert.entity.AlertStatus;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import kr.ssok.ssom.backend.domain.alert.repository.AlertRepository;
import kr.ssok.ssom.backend.domain.alert.repository.AlertStatusRepository;
import kr.ssok.ssom.backend.domain.alert.service.kafka.AlertKafkaProducer;
import kr.ssok.ssom.backend.domain.alert.dto.kafka.AlertCreatedEvent;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.repository.UserRepository;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertPerformanceTestServiceImpl implements AlertPerformanceTestService {

    private final AlertRepository alertRepository;
    private final AlertStatusRepository alertStatusRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;
    private final AlertKafkaProducer alertKafkaProducer;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional
    public Map<String, Object> processSyncGrafanaAlert(AlertGrafanaRequestDto requestDto, int targetUserCount) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<AlertRequestDto> alertList = requestDto.getAlerts();
            List<User> targetUsers = getTargetUsers(targetUserCount);
            
            for (AlertRequestDto alertRequest : alertList) {
                processSingleAlertSync(alertRequest, AlertKind.GRAFANA, targetUsers);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            result.put("alertCount", alertList.size());
            result.put("targetUserCount", targetUsers.size());
            result.put("processingTimeMs", processingTime);
            result.put("success", true);
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> processSyncOpensearchAlert(String requestStr, int targetUserCount) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<AlertRequestDto> alertList = parseRawStringToDtoList(requestStr);
            List<User> targetUsers = getTargetUsers(targetUserCount);
            
            for (AlertRequestDto alertRequest : alertList) {
                processSingleAlertSync(alertRequest, AlertKind.OPENSEARCH, targetUsers);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            result.put("alertCount", alertList.size());
            result.put("targetUserCount", targetUsers.size());
            result.put("processingTimeMs", processingTime);
            result.put("success", true);
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> processSyncDevopsAlert(AlertDevopsRequestDto requestDto, int targetUserCount) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            String[] appParts = requestDto.getApp().split("_");
            AlertKind devopsKind = AlertKind.valueOf(appParts[0].toUpperCase());
            
            AlertRequestDto alertRequest = AlertRequestDto.builder()
                    .id(devopsKind + "_test_" + System.currentTimeMillis())
                    .level(requestDto.getLevel())
                    .app(appParts[1])
                    .timestamp(requestDto.getTimestamp())
                    .message(requestDto.getMessage())
                    .build();

            List<User> targetUsers = getTargetUsers(targetUserCount);
            processSingleAlertSync(alertRequest, devopsKind, targetUsers);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            result.put("alertCount", 1);
            result.put("targetUserCount", targetUsers.size());
            result.put("processingTimeMs", processingTime);
            result.put("success", true);
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> processAsyncGrafanaAlert(AlertGrafanaRequestDto requestDto, int targetUserCount) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<AlertRequestDto> alertList = requestDto.getAlerts();
            List<User> targetUsers = getTargetUsers(targetUserCount);
            
            for (AlertRequestDto alertRequest : alertList) {
                processSingleAlertAsync(alertRequest, AlertKind.GRAFANA);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            result.put("alertCount", alertList.size());
            result.put("targetUserCount", targetUsers.size());
            result.put("processingTimeMs", processingTime);
            result.put("success", true);
            result.put("note", "Alert 저장 완료, AlertStatus 생성 및 알림 전송은 Kafka를 통해 비동기 처리 중");
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> processAsyncOpensearchAlert(String requestStr, int targetUserCount) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<AlertRequestDto> alertList = parseRawStringToDtoList(requestStr);
            List<User> targetUsers = getTargetUsers(targetUserCount);
            
            for (AlertRequestDto alertRequest : alertList) {
                processSingleAlertAsync(alertRequest, AlertKind.OPENSEARCH);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            result.put("alertCount", alertList.size());
            result.put("targetUserCount", targetUsers.size());
            result.put("processingTimeMs", processingTime);
            result.put("success", true);
            result.put("note", "Alert 저장 완료, AlertStatus 생성 및 알림 전송은 Kafka를 통해 비동기 처리 중");
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> processAsyncDevopsAlert(AlertDevopsRequestDto requestDto, int targetUserCount) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            String[] appParts = requestDto.getApp().split("_");
            AlertKind devopsKind = AlertKind.valueOf(appParts[0].toUpperCase());
            
            AlertRequestDto alertRequest = AlertRequestDto.builder()
                    .id(devopsKind + "_test_" + System.currentTimeMillis())
                    .level(requestDto.getLevel())
                    .app(appParts[1])
                    .timestamp(requestDto.getTimestamp())
                    .message(requestDto.getMessage())
                    .build();

            List<User> targetUsers = getTargetUsers(targetUserCount);
            processSingleAlertAsync(alertRequest, devopsKind);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            result.put("alertCount", 1);
            result.put("targetUserCount", targetUsers.size());
            result.put("processingTimeMs", processingTime);
            result.put("success", true);
            result.put("note", "Alert 저장 완료, AlertStatus 생성 및 알림 전송은 Kafka를 통해 비동기 처리 중");
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    public List<Object> generateTestData(String alertType, int count) {
        List<Object> testData = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            switch (alertType.toLowerCase()) {
                case "grafana":
                    testData.add(generateGrafanaTestData(i));
                    break;
                case "opensearch":
                    testData.add(generateOpensearchTestData(i));
                    break;
                case "devops":
                    testData.add(generateDevopsTestData(i));
                    break;
            }
        }
        return testData;
    }

    @Override
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("activeSseConnections", alertService.getActiveEmitterCount());
            status.put("totalUsers", userRepository.count());
            status.put("systemTime", LocalDateTime.now());
            
            return status;
        } catch (Exception e) {
            status.put("error", e.getMessage());
            return status;
        }
    }

    @Override
    public Map<String, Object> getPerformanceMetrics(int durationMinutes) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timeRangeMinutes", durationMinutes);
        metrics.put("timestamp", LocalDateTime.now());
        return metrics;
    }

    @Override
    public Map<String, Object> resetTestEnvironment() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Alert> testAlerts = alertRepository.findByIdContaining("test");
            for (Alert alert : testAlerts) {
                alertStatusRepository.deleteByAlert_AlertId(alert.getAlertId());
            }
            alertRepository.deleteAll(testAlerts);
            
            result.put("deletedTestAlerts", testAlerts.size());
            result.put("timestamp", LocalDateTime.now());
            result.put("success", true);
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    // 헬퍼 메서드들
    private List<User> getTargetUsers(int targetUserCount) {
        List<User> allUsers = userRepository.findAll();
        if (targetUserCount <= 0 || targetUserCount >= allUsers.size()) {
            return allUsers;
        } else {
            return allUsers.subList(0, Math.min(targetUserCount, allUsers.size()));
        }
    }

    @Transactional
    protected void processSingleAlertSync(AlertRequestDto request, AlertKind kind, List<User> targetUsers) {
        Alert alert = Alert.builder()
                .id(request.getId())
                .title("[" + request.getLevel() + "] " + request.getApp())
                .message(request.getMessage())
                .kind(kind)
                .timestamp(request.getTimestamp())
                .build();
        alertRepository.save(alert);

        List<AlertStatus> statusList = new ArrayList<>();
        for (User user : targetUsers) {
            AlertStatus status = AlertStatus.builder()
                    .alert(alert)
                    .user(user)
                    .isRead(false)
                    .build();
            statusList.add(status);
        }
        alertStatusRepository.saveAll(statusList);

        for (AlertStatus status : statusList) {
            AlertResponseDto responseDto = AlertResponseDto.from(status);
            alertService.sendAlertToUser(status.getUser().getId(), responseDto);
        }
    }

    @Transactional
    protected Long processSingleAlertAsync(AlertRequestDto request, AlertKind kind) {
        Alert alert = Alert.builder()
                .id(request.getId())
                .title("[" + request.getLevel() + "] " + request.getApp())
                .message(request.getMessage())
                .kind(kind)
                .timestamp(request.getTimestamp())
                .build();
        Alert savedAlert = alertRepository.save(alert);

        AlertCreatedEvent event = AlertCreatedEvent.of(
                savedAlert.getAlertId(), 
                kind, 
                request.getApp()
        );
        alertKafkaProducer.publishAlertCreated(event);

        return savedAlert.getAlertId();
    }

    private List<AlertRequestDto> parseRawStringToDtoList(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                throw new BaseException(BaseResponseStatus.PARSING_ERROR);
            }

            String fixed = raw.trim().replaceAll(",\\s*]", "]");
            if (!fixed.trim().startsWith("[")) {
                throw new BaseException(BaseResponseStatus.PARSING_ERROR);
            }

            return objectMapper.readValue(fixed, new TypeReference<List<AlertRequestDto>>() {});
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.PARSING_ERROR);
        }
    }

    private AlertGrafanaRequestDto generateGrafanaTestData(int index) {
        AlertRequestDto alertRequest = AlertRequestDto.builder()
                .id("grafana_test_" + System.currentTimeMillis() + "_" + index)
                .level("CRITICAL")
                .app("test-app-" + (index % 3 + 1))
                .timestamp(String.valueOf(LocalDateTime.now()))
                .message("테스트용 그라파나 알림 메시지 #" + index)
                .build();

        AlertGrafanaRequestDto grafanaRequest = new AlertGrafanaRequestDto();
        grafanaRequest.setAlerts(Arrays.asList(alertRequest));
        return grafanaRequest;
    }

    private String generateOpensearchTestData(int index) {
        AlertRequestDto alertRequest = AlertRequestDto.builder()
                .id("opensearch_test_" + System.currentTimeMillis() + "_" + index)
                .level("WARNING")
                .app("search-service-" + (index % 2 + 1))
                .timestamp(String.valueOf(LocalDateTime.now()))
                .message("테스트용 오픈서치 알림 메시지 #" + index)
                .build();

        try {
            return objectMapper.writeValueAsString(Arrays.asList(alertRequest));
        } catch (Exception e) {
            return "[]";
        }
    }

    private AlertDevopsRequestDto generateDevopsTestData(int index) {
        String[] apps = {"jenkins_test-service", "argocd_deploy-app"};
        String[] levels = {"SUCCESS", "FAILURE", "UNSTABLE"};
        
        return AlertDevopsRequestDto.builder()
                .app(apps[index % apps.length])
                .level(levels[index % levels.length])
                .message("테스트용 DevOps 알림 메시지 #" + index)
                .timestamp(String.valueOf(LocalDateTime.now()))
                .build();
    }
}
