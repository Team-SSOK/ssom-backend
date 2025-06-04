package kr.ssok.ssom.backend.domain.alert.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import kr.ssok.ssom.backend.domain.alert.dto.*;
import kr.ssok.ssom.backend.domain.alert.entity.Alert;
import kr.ssok.ssom.backend.domain.alert.entity.AlertStatus;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import kr.ssok.ssom.backend.domain.alert.repository.AlertRepository;
import kr.ssok.ssom.backend.domain.alert.repository.AlertStatusRepository;
import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.repository.UserRepository;
import kr.ssok.ssom.backend.global.client.FirebaseClient;
import kr.ssok.ssom.backend.global.dto.FcmMessageRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AlertServiceImpl implements AlertService {
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 1시간
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    private final RedisTemplate<String, String> redisTemplate;
    private final FirebaseClient firebaseClient;

    private final AlertRepository alertRepository;
    private final AlertStatusRepository alertStatusRepository;
    private final UserRepository userRepository;

    /*
    * 알림 SSE 구독
    * */
    public SseEmitter subscribe(String employeeId, String lastEventId, HttpServletResponse response){
        log.info("[알림 SSE 구독] 서비스 진입");

        String emitterId = employeeId;
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(emitterId, emitter);

        response.setHeader("X-Accel-Buffering", "no");

        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));
        emitter.onError((e) -> emitters.remove(emitterId));

        try {
            String eventId = createTimeIncludeId(emitterId);
            emitter.send(SseEmitter.event().id(eventId).name("SSE_ALERT_INIT").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitterId);
            emitter.completeWithError(e);

            log.error("SSE_ALERT_INIT failed");
            throw new RuntimeException("SSE_ALERT_INIT failed" + e);
        }

        log.info("sse 연결 완료");

        return emitter;
    }

    private String createTimeIncludeId(String employeeId) {
        return employeeId + "_" + System.currentTimeMillis();
    }


    /*
     * 전체 알림 목록 조회
     * */
    @Override
    public List<AlertResponseDto> getAllAlertsForUser(String employeeId) {
        log.info("[전체 알림 목록 조회] 서비스 진입");

        return alertStatusRepository.findByUser_Id(employeeId)
                .stream()
                .map(AlertResponseDto::from)
                .collect(Collectors.toList());
    }

    /*
     * 알림 개별 상태 변경
     * */
    @Override
    public void modifyAlertStatus(AlertModifyRequestDto request) {
        log.info("[알림 개별 상태 변경] 서비스 진입");

        AlertStatus status = alertStatusRepository.findById(request.getAlertStatusId())
                .orElseThrow(() -> new RuntimeException("AlertStatus not found"));
        if (request.isRead()) {
            status.markAsRead();
        } else {
            status.markAsUnread();
        }
    }

    /*
     * 알림 개별 삭제
     * */
    @Override
    public void deleteAlert(AlertModifyRequestDto request) {
        log.info("[알림 개별 삭제] 서비스 진입");

        // AlertStatus 조회
        AlertStatus status = alertStatusRepository.findById(request.getAlertStatusId())
                .orElseThrow(() -> new RuntimeException("AlertStatus not found"));

        // AlertStatus 삭제
        alertStatusRepository.delete(status);
        log.info("[알림 개별 삭제] AlertStatus 삭제 완료");

        /*
        // Alert 조회
        Alert alert = status.getAlert();

        // Alert 삭제
        alertRepository.delete(alert);
        */
    }
    /******************************************************************************************************/

    /*
    * 알림 저장 및 전송
    * */
    @Override
    public void createAlert(AlertRequestDto request, AlertKind kind) {
        log.info("[알림 생성] 알림 저장 및 전송 진행 중 ...");

        // 1. Alert 저장
        Alert alert = Alert.builder()
                .title("[" + request.getLevel() + "] " + request.getApp())  // [ERROR] ssok-bank
                .message(request.getMessage())                              // Authentication error: Authorization header is missing or invalid
                .kind(kind)
                .timestamp(request.getTimestamp())
                .build();
        alertRepository.save(alert);

        // 2. 전체 사용자 가져오기
        List<User> users = userRepository.findAll();

        // 3. 조건에 맞는 사용자 필터링
        List<User> filteredUsers = users.stream()
                .filter(user -> {
                    Department dept = user.getDepartment();
                    String app = request.getApp();

                    // 운영팀, 대외팀은 무조건 받음
                    if (dept == Department.OPERATION || dept == Department.EXTERNAL) {
                        return true;
                    }

                    // 계정팀(Core Bank) : 출처 app이 ssok-bank 일 때만
                    if (dept == Department.CORE_BANK) {
                        return "ssok-bank".equalsIgnoreCase(app);
                    }

                    // 채널팀(Channel) : 출처 app이 ssok-bank 이 아닌 경우
                    if (dept == Department.CHANNEL) {
                        return !"ssok-bank".equalsIgnoreCase(app);
                    }

                    // 기본적으로는 받지 않음
                    return false;
                })
                .collect(Collectors.toList());

        // 4. 각 필터링된 사용자에게 AlertStatus 생성
        List<AlertStatus> statusList = new ArrayList<>();
        for (User user : filteredUsers) {
            AlertStatus status = AlertStatus.builder()
                    .alert(alert)
                    .user(user)
                    .isRead(false)
                    .build();
            statusList.add(status);
        }
        alertStatusRepository.saveAll(statusList);
        log.info("[알림 생성] 알림 저장 완료");

        // 5. 알림 전송용 DTO로 변환 후 반환
        List<AlertResponseDto> dtoList = statusList.stream()
                .map(AlertResponseDto::from)
                .collect(Collectors.toList());

        // 6. 알림 푸시
        for (AlertResponseDto dto : dtoList) {
            sendSseAlertToUser(dto.getEmployeeId(), dto);
        }

    }

    /*
    * 알림 분기
    * */
    public void sendAlertToUser(String employeeId, AlertResponseDto alertResponseDto) {
        SseEmitter emitter = emitters.get(employeeId);

        if (emitter != null) {
            // 앱 내부 → SSE 전송
            try {
                emitter.send(SseEmitter.event()
                        .name("SSE_ALERT")
                        .data(alertResponseDto));
                log.info("[SSE 전송 성공] employeeId={}", employeeId);
            } catch (IOException e) {
                emitters.remove(employeeId);
                log.error("[SSE 전송 실패, FCM으로 전환] employeeId={}", employeeId, e);
                // SSE 실패 시 FCM 전송 시도
                sendFcmNotification(employeeId, alertResponseDto.getTitle(), alertResponseDto.getMessage());
            }
        } else {
            // 앱 외부 → FCM 전송
            log.info("[앱 외부 감지, FCM 전송] employeeId={}", employeeId);
            sendFcmNotification(employeeId, alertResponseDto.getTitle(), alertResponseDto.getMessage());
        }
    }

    /*
     * 알림 SSE 전송
     * */
    public void sendSseAlertToUser(String emitterId, AlertResponseDto alertResponseDto) {
        log.info("[알림 SSE 전송] 서비스 진입");

        SseEmitter emitter = emitters.get(emitterId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("SSE_ALERT")
                        .data(alertResponseDto));
            } catch (IOException e) {
                emitters.remove(emitterId);
                log.error("SSE_ALERT failed");
                throw new RuntimeException("SSE_ALERT failed" + e);
            }
        }
        log.info("[알림 SSE 전송] 처리 완료.");
    }

    /*
    * 알림 FCM 구현
    * */
    public void sendFcmNotification(String employeeId, String title, String body) {
        try {
            String token = redisTemplate.opsForValue().get("userfcm:" + employeeId);

            if (token == null) {
                log.warn("FCM 토큰이 존재하지 않습니다: employeeId={}", employeeId);
                return;
            }

            // FCM 메시지 요청 생성
            FcmMessageRequestDto request = FcmMessageRequestDto.builder()
                    .title(title)
                    .body(body)
                    .token(token)
                    .build();

            // FCM 클라이언트로 메시지 전송
            firebaseClient.sendNotification(request);
        } catch (DataAccessException e) {
            throw new RuntimeException("REDIS_ACCESS_FAILED", e);
        }
    }

    @Override
    public void createGrafanaAlert(AlertGrafanaRequestDto requestDto) {
        log.info("[그라파나 알림] 서비스 진입");

        List<AlertRequestDto> alertList = requestDto.getAlerts();

        if (alertList == null || alertList.isEmpty()) {
            log.warn("[그라파나 알림] 전달받은 알림 리스트가 비어있습니다.");
            return;
        }

        for (AlertRequestDto alertRequest : alertList) {
            createAlert(alertRequest, AlertKind.GRAFANA);
        }

        log.info("[그라파나 알림] 전체 {}건 서비스 처리 완료", alertList.size());
    }

    @Override
    public void createOpensearchAlert(AlertOpensearchRequestDto requestDto) {
        log.info("[오픈서치 대시보드 알림] 서비스 진입");
        
        String rawJson = requestDto.getRequest();
        List<AlertRequestDto> alertList = parseRawStringToDtoList(rawJson);

        if (alertList == null || alertList.isEmpty()) {
            log.warn("[오픈서치 대시보드 알림] 전달받은 알림 리스트가 비어있습니다.");
            return;
        }

        for (AlertRequestDto alertRequest : alertList) {
            createAlert(alertRequest, AlertKind.OPENSEARCH);
        }
        log.info("[오픈서치 대시보드 알림] 서비스 처리 완료");
    }

    private List<AlertRequestDto> parseRawStringToDtoList(String raw) {
        log.info("[JSON Parsing] 진행 중 ...");

        try {
            // 1. 중첩된 중괄호 제거: "{{" → "{", "}}" → "}"
            String fixed = raw.replaceAll("\\{\\s*\\{", "\\{")
                    .replaceAll("}}", "}");

            // 2. 마지막 쉼표 제거
            fixed = fixed.trim();
            if (fixed.endsWith(",")) {
                fixed = fixed.substring(0, fixed.length() - 1);
            }

            // 3. JSON 배열 형태로 감싸기
            // 여러 개의 객체가 있을 경우 각 객체를 `}, {` 로 구분하므로
            // 이걸 이용해서 안전하게 나누고 다시 배열화
            if (!fixed.startsWith("[") && !fixed.endsWith("]")) {
                // 객체들 추출
                String[] objects = fixed.split("},\\s*\\{");
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (int i = 0; i < objects.length; i++) {
                    String obj = objects[i].trim();

                    // 맨 앞/뒤 중괄호 정리
                    if (!obj.startsWith("{")) sb.append("{");
                    sb.append(obj);
                    if (!obj.endsWith("}")) sb.append("}");

                    if (i != objects.length - 1) sb.append(",");
                }
                sb.append("]");
                fixed = sb.toString();
            }

            return objectMapper.readValue(fixed, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("[JSON Parsing] JSON Parsing 실패", e);
            throw new RuntimeException("JSON Parsing 실패", e);
        }
    }

    @Override
    public void createIssueAlert(AlertIssueRequestDto requestDto) {
        log.info("[이슈 생성 알림] 서비스 진입");

        // 1. Alert 저장
        Alert alert = Alert.builder()
                .title("[ISSUE] 이슈 공유")
                .message("새로운 이슈가 공유되었습니다.")
                .kind(AlertKind.ISSUE)
                .build();
        alertRepository.save(alert);

        // 2. 알림 공유 대상자 목록 가져오기
        List<User> targetUsers = new ArrayList<>();
        if (requestDto.getSharedEmployeeIds() != null && !requestDto.getSharedEmployeeIds().isEmpty()) {
            targetUsers = userRepository.findAllById(requestDto.getSharedEmployeeIds());
        }

        // 3. AlertStatus 생성 및 SSE 알림 전송
        for (User user : targetUsers) {
            AlertStatus alertStatus = AlertStatus.builder()
                    .alert(alert)
                    .user(user)
                    .isRead(false)
                    .build();
            alertStatusRepository.save(alertStatus);

            // 알림 전송
            AlertResponseDto responseDto = AlertResponseDto.from(alertStatus);
            sendAlertToUser(user.getId(), responseDto);
        }

        log.info("[이슈 생성 알림] 서비스 처리 완료");
    }

    @Override
    public void createDevopsAlert(AlertSendRequestDto requestDto) {
        log.info("[DevOps 알림 생성] 서비스 진입");

        // 1. app에서 alertKind와 appName 파싱
        String[] appParts = requestDto.getApp().split("_");
        if (appParts.length != 2) {
            throw new IllegalArgumentException("[DevOps 알림 생성] 잘못된 app 형식입니다. 예: jenkins_ssok-bank");
        }

        String kindStr = appParts[0].toUpperCase(); // "JENKINS", "ARGOCD"
        String appName = appParts[1];               // "ssok-bank"

        AlertKind devopsKind;
        try {
            devopsKind = AlertKind.valueOf(kindStr);
            AlertRequestDto alertRequest = AlertRequestDto.builder()
                    .id(devopsKind + "_noNeedId")
                    .level(requestDto.getLevel())
                    .app(appName)
                    .timestamp(requestDto.getTimestamp())
                    .message(requestDto.getMessage())
                    .build();

            createAlert(alertRequest, devopsKind);

        } catch (IllegalArgumentException e) {
            log.error("[DevOps 알림 생성] 실패");
            throw new RuntimeException("[DevOps 알림 생성] 지원하지 않는 AlertKind입니다: " + kindStr);
        }

        log.info("[DevOps 알림 생성] 서비스 처리 완료");
    }
}
