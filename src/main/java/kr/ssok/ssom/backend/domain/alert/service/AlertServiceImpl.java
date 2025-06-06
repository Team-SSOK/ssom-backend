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
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

    /**
     * 알림 SSE 구독
     *
     * @param employeeId 사용자 고유 식별자
     * @param lastEventId 마지막 이벤트 ID (클라이언트 재연결 시)
     * @param response HTTP 응답
     * @return SseEmitter 객체
     */
    public SseEmitter subscribe(String employeeId, String lastEventId, HttpServletResponse response){
        log.info("[알림 SSE 구독] 서비스 진입 : employeeId = {}, lastEventId = {}", employeeId, lastEventId);

        // 1. 유효성 검사
        if (employeeId == null || employeeId.trim().isEmpty()) {
            log.error("[알림 SSE 구독] 오류 : employeeId = {}", employeeId);
            throw new BaseException(BaseResponseStatus.SSE_BAD_REQUEST);

        }

        // 2. emitterId 생성 (고유 식별자)
        String emitterId = employeeId;

        // 3. 기존 emitter 존재 시 제거
        if (emitters.containsKey(emitterId)) {
            log.warn("[알림 SSE 구독] 기존 emitter 존재. 제거 후 새 emitter 생성 : emitterId = {}", emitterId);
            SseEmitter oldEmitter = emitters.remove(emitterId);
            if (oldEmitter != null) oldEmitter.complete();
        }

        // 4. emitter 생성 및 등록
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(emitterId, emitter);

        log.info("[알림 SSE 구독] 연결 완료 : employeeId = {}, emitterId = {}", employeeId, emitterId);

        response.setHeader("X-Accel-Buffering", "no");

        // 5. emitter 이벤트 콜백 등록
        emitter.onCompletion(() -> {
            log.info("[Emitter 완료] emitterId = {}", emitterId);
            emitters.remove(emitterId);
        });
        emitter.onTimeout(() -> {
            log.info("[Emitter 타임아웃] emitterId = {}", emitterId);
            emitters.remove(emitterId);
        });
        emitter.onError((e) -> {
            log.error("[Emitter 오류 발생] emitterId = {}, error = {}", emitterId, e.getMessage(), e);
            emitters.remove(emitterId);
        });

        // 6. 클라이언트에 연결 초기 이벤트 전송
        try {
            String eventId = createTimeIncludeId(emitterId);
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("SSE_ALERT_INIT")
                    .data("connected"));

            log.info("[알림 SSE 구독] SSE 초기 이벤트 전송 성공 : emitterId = {}, eventId = {}", emitterId, eventId);

        } catch (IOException | IllegalStateException e) {
            emitters.remove(emitterId);
            emitter.completeWithError(e);

            log.error("[알림 SSE 구독] 오류 : SSE_ALERT_INIT failed  - emitterId = {}, error = {}", emitterId, e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.SSE_INIT_ERROR);
        }

        log.info("[알림 SSE 구독] SSE 연결 완료 : emitterId = {}", emitterId);

        return emitter;
    }

    private String createTimeIncludeId(String employeeId) {
        return employeeId + "_" + System.currentTimeMillis();
    }

    /**
     * 전체 알림 목록 조회
     *
     * @param employeeId 사용자 고유 식별자
     * @return List<AlertResponseDto>
     */
    @Override
    public List<AlertResponseDto> getAllAlertsForUser(String employeeId) {
        log.info("[전체 알림 목록 조회] 서비스 진입 : employeeId = {}", employeeId);

        // 1. 유효성 검사
        if (employeeId == null || employeeId.trim().isEmpty()) {
            log.error("[전체 알림 목록 조회] 오류 : 잘못된 employeeId = {}", employeeId);
            throw new BaseException(BaseResponseStatus.BAD_REQUEST);
        }

        try {
            // 2. 알림 목록 조회
            List<AlertStatus> alertStatusList = alertStatusRepository.findByUser_Id(employeeId);

            if (alertStatusList.isEmpty()) {
                log.info("[전체 알림 목록 조회] 알림 없음 : employeeId = {}", employeeId);
            }

            return alertStatusList.stream()
                    .map(AlertResponseDto::from)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[전체 알림 목록 조회] 오류 발생 : employeeId = {}, error = {}", employeeId, e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 알림 개별 상태 변경
     *
     * @param request AlertModifyRequestDto
     */
    @Override
    public void modifyAlertStatus(AlertModifyRequestDto request) {
        log.info("[알림 개별 상태 변경] 서비스 진입 : request = {}", request);

        // 1. 요청값 검증
        if (request == null || request.getAlertStatusId() == null) {
            log.error("[알림 개별 상태 변경] 잘못된 요청 : request 또는 alertStatusId가 null");
            throw new BaseException(BaseResponseStatus.BAD_REQUEST);
        }

        try {
            // 2. 알림 상태 조회
            AlertStatus status = alertStatusRepository.findById(request.getAlertStatusId())
                    .orElseThrow(() -> {
                        log.error("[알림 개별 상태 변경] 해당 알림 상태를 찾을 수 없음 : alertStatusId = {}", request.getAlertStatusId());
                        return new BaseException(BaseResponseStatus.NOT_FOUND_ALERT);
                    });

            // 3. 읽음/안읽음 상태 처리
            if (request.isRead()) {
                status.markAsRead();
            } else {
                status.markAsUnread();
            }

            log.info("[알림 개별 상태 변경] 변경 완료 : alertStatusId = {}, isRead = {}", request.getAlertStatusId(), request.isRead());

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[알림 개별 상태 변경] 처리 중 예외 발생 : alertStatusId = {}, error = {}", request.getAlertStatusId(), e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 알림 개별 삭제
     *
     *  @param request AlertModifyRequestDto
     */
    @Override
    public void deleteAlert(AlertModifyRequestDto request) {
        log.info("[알림 개별 삭제] 서비스 진입 : request = {}", request);

        // 1. 요청값 검증
        if (request == null || request.getAlertStatusId() == null) {
            log.error("[알림 개별 삭제] 잘못된 요청 : request 또는 alertStatusId가 null");
            throw new BaseException(BaseResponseStatus.BAD_REQUEST);
        }

        try {
            // 2. 알림 상태 조회
            AlertStatus status = alertStatusRepository.findById(request.getAlertStatusId())
                    .orElseThrow(() -> {
                        log.error("[알림 개별 삭제] 해당 알림 상태를 찾을 수 없음 : alertStatusId = {}", request.getAlertStatusId());
                        return new BaseException(BaseResponseStatus.NOT_FOUND_ALERT);
                    });

            // 3. 알림 상태 삭제
            alertStatusRepository.delete(status);
            log.info("[알림 개별 삭제] AlertStatus 삭제 완료 : alertStatusId = {}", request.getAlertStatusId());

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[알림 개별 삭제] 처리 중 예외 발생 : alertStatusId = {}, error = {}", request.getAlertStatusId(), e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 그라파나 알림 처리
     *      1. 공통 포맷에 담아 createAlert 전송
     *
     * @param requestDto : Json, 리스트
     */
    @Override
    public void createGrafanaAlert(AlertGrafanaRequestDto requestDto) {
        log.info("[그라파나 알림] 서비스 진입 : requestDto = {}", requestDto);

        try {
            // 1. 요청값 검증
            if (requestDto == null || requestDto.getAlerts() == null || requestDto.getAlerts().isEmpty()) {
                log.warn("[그라파나 알림] 전달받은 알림 리스트가 비어있습니다.");
                return;
            }

            List<AlertRequestDto> alertList = requestDto.getAlerts();

            // 2. 알림 처리
            for (AlertRequestDto alertRequest : alertList) {
                try {
                    createAlert(alertRequest, AlertKind.GRAFANA);
                } catch (BaseException be) {
                    log.error("[그라파나 알림] 개별 알림 처리 실패 : alertRequest = {}, error = {}", alertRequest, be.getMessage());
                } catch (Exception e) {
                    log.error("[그라파나 알림] 알림 처리 중 예외 발생 : alertRequest = {}, error = {}", alertRequest, e.getMessage(), e);
                }
            }

            log.info("[그라파나 알림] 전체 {}건 서비스 처리 완료", alertList.size());

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[그라파나 알림] 전체 처리 중 예외 발생 : error = {}", e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 오픈서치 대시보드 알림 처리
     *      1. String으로 받은 데이터 Json으로 parsing하여 공통 포맷에 담기
     *      2. createAlert 전송
     *
     * @param requestStr : String, 리스트
     */
    @Override
    public void createOpensearchAlert(String requestStr) {
        log.info("[오픈서치 대시보드 알림] 서비스 진입 : requestStr = {}", requestStr);

        try {
            if (requestStr == null || requestStr.isEmpty()) {
                log.warn("[오픈서치 대시보드 알림] 전달받은 원본 데이터가 비어있습니다.");
                return;
            }

            List<AlertRequestDto> alertList = parseRawStringToDtoList(requestStr);

            if (alertList == null || alertList.isEmpty()) {
                log.warn("[오픈서치 대시보드 알림] Json 파싱 결과 알림 리스트가 비어있습니다.");
                throw new BaseException(BaseResponseStatus.PARSING_ERROR);
            }

            for (AlertRequestDto alertRequest : alertList) {
                try {
                    createAlert(alertRequest, AlertKind.OPENSEARCH);
                } catch (BaseException be) {
                    log.error("[오픈서치 대시보드 알림] 개별 알림 처리 실패 : alertRequest = {}, error = {}", alertRequest, be.getMessage());
                } catch (Exception e) {
                    log.error("[오픈서치 대시보드 알림] 알림 처리 중 예외 발생 : alertRequest = {}, error = {}", alertRequest, e.getMessage(), e);
                }
            }

            log.info("[오픈서치 대시보드 알림] 전체 {}건 서비스 처리 완료", alertList.size());

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[오픈서치 대시보드 알림] 전체 처리 중 예외 발생 - error = {}", e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<AlertRequestDto> parseRawStringToDtoList(String raw) {
        log.info("[JSON Parsing] 진행 중 ...");

        try {
            if (raw == null || raw.isBlank()) {
                log.warn("[JSON Parsing] 전달받은 원본 문자열이 비어있습니다.");
                throw new BaseException(BaseResponseStatus.PARSING_ERROR);
            }

            // 공백 및 개행 제거
            String fixed = raw.trim();
            fixed = fixed.replaceAll(",\\s*]", "]");

            if (!fixed.trim().startsWith("[")) {
                log.warn("[JSON Parsing] 전달받은 원본 문자열의 형식이 상이합니다.");
                throw new BaseException(BaseResponseStatus.PARSING_ERROR);
            }

            return objectMapper.readValue(fixed, new TypeReference<List<AlertRequestDto>>() {});

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[JSON Parsing] JSON 파싱 중 예외 발생 : input = {}", raw, e);
            throw new BaseException(BaseResponseStatus.PARSING_ERROR);
        }
    }

    /**
     * 이슈 알림 처리
     *      1. createAlert 전송하지 않음 (user가 지정돼있으므로.)
     *
     * @param requestDto : String, 리스트
     */
    @Override
    public void createIssueAlert(AlertIssueRequestDto requestDto) {
        log.info("[이슈 생성 알림] 서비스 진입 : requestDto = {}", requestDto);

        try {
            // 1. Alert 저장
            Alert alert = Alert.builder()
                    .id(AlertKind.ISSUE + "_noNeedId")
                    .title("[ISSUE] 이슈 공유")
                    .message("새로운 이슈가 공유되었습니다.")
                    .kind(AlertKind.ISSUE)
                    .timestamp(requestDto.getTimestamp().toString())
                    .build();
            alertRepository.save(alert);

            // 2. 알림 대상자 조회
            List<User> targetUsers = new ArrayList<>();
            //List<String> sharedIds = requestDto.getSharedEmployeeIds();
            List<String> sharedIds = requestDto.getAssigneeGithubIds();

            if (sharedIds != null && !sharedIds.isEmpty()) {
                //targetUsers = userRepository.findAllById(sharedIds);
                targetUsers = userRepository.findAllByGithubIdIn(sharedIds);

                if (targetUsers.isEmpty()) {
                    log.warn("[이슈 생성 알림] 공유 대상자가 존재하지 않음: {}", sharedIds);
                    throw new BaseException(BaseResponseStatus.ALERT_TARGET_USER_NOT_FOUND);
                }
            } else {
                log.warn("[이슈 생성 알림] 공유 대상자가 지정되지 않음");
                throw new BaseException(BaseResponseStatus.ALERT_TARGET_USER_NOT_FOUND);
            }

            // 3. AlertStatus 생성 및 전송
            for (User user : targetUsers) {
                AlertStatus alertStatus = AlertStatus.builder()
                        .alert(alert)
                        .user(user)
                        .isRead(false)
                        .build();
                alertStatusRepository.save(alertStatus);

                AlertResponseDto responseDto = AlertResponseDto.from(alertStatus);
                sendAlertToUser(user.getId(), responseDto);
            }

            log.info("[이슈 생성 알림] 서비스 처리 완료");
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[이슈 생성 알림] 처리 중 예외 발생", e);
            throw new BaseException(BaseResponseStatus.ALERT_CREATE_FAILED);
        }
    }

    /**
     * Devops (Jenkins 및 ArgoCD) 알림 처리
     *      1. app 에서 alertKind와 appName에 대해 parsing
     *      2. 공통 포맷에 담기
     *      3. createAlert 전송
     *
     * @param requestDto : Json, 단건
     */
    @Override
    public void createDevopsAlert(AlertDevopsRequestDto requestDto) {
        log.info("[Devops 알림 생성] 서비스 진입 : requestDto.getApp() = {}", requestDto.getApp());

        if (requestDto == null || requestDto.getApp() == null || requestDto.getApp().trim().isEmpty()) {
            log.error("[Devops 알림 생성] requestDto 또는 app 필드가 null 또는 빈 값입니다.");
            throw new BaseException(BaseResponseStatus.INVALID_REQUEST);
        }

        // 1. app에서 alertKind와 appName 파싱
        String[] appParts = requestDto.getApp().split("_");
        if (appParts.length != 2) {
            log.error("[Devops 알림 생성] 잘못된 app 형식입니다. 예: jenkins_ssok-bank, app={}", requestDto.getApp());
            throw new BaseException(BaseResponseStatus.INVALID_REQUEST);
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
            log.error("[Devops 알림 생성] 지원하지 않는 AlertKind입니다: {}", kindStr, e);
            throw new BaseException(BaseResponseStatus.UNSUPPORTED_ALERT_KIND);
        } catch (Exception e) {
            log.error("[Devops 알림 생성] 알림 생성 중 예외 발생", e);
            throw new BaseException(BaseResponseStatus.ALERT_CREATE_FAILED);
        }

        log.info("[Devops 알림 생성] 서비스 처리 완료");
    }

    /**
     * 알림 저장 및 전송
     *
     * @param request
     * @param kind
     */
    @Override
    public void createAlert(AlertRequestDto request, AlertKind kind) {
        log.info("[알림 생성] 알림 저장 및 전송 진행 중 ...");

        try {
            // 1. Alert 저장
            Alert alert = Alert.builder()
                    .id(request.getId())
                    .title("[" + request.getLevel() + "] " + request.getApp())  // [ERROR] ssok-bank
                    .message(request.getMessage())                              // Authentication error: Authorization header is missing or invalid
                    .kind(kind)                                                 // OPENSEARCH
                    .timestamp(request.getTimestamp())                          // 2025-05-30T08:37:50.772492854+00:00
                    .build();
            alertRepository.save(alert);

            // 2. 전체 사용자 가져오기
            List<User> users = userRepository.findAll();

            // 3. 조건에 맞는 사용자 필터링 -> TODO : 부서 별 조회 권한 refactoring 권장
            List<User> filteredUsers = users.stream()
                    .filter(user -> {
                        Department dept = user.getDepartment();
                        String app = request.getApp();
                        String lowerApp = app != null ? app.toLowerCase() : "";

                        if (dept == Department.OPERATION || dept == Department.EXTERNAL) return true;
                        if (dept == Department.CORE_BANK) return lowerApp.contains("ssok-bank");
                        if (dept == Department.CHANNEL) return !lowerApp.contains("ssok-bank");

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
                sendAlertToUser(dto.getEmployeeId(), dto);
            }

        } catch (Exception e) {
            log.error("[알림 생성] 알림 생성 및 전송 중 예외 발생: {}", e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.ALERT_CREATE_FAILED);
        }

    }

    /**
     *  알림 분기
     *
     * @param employeeId
     * @param responseDto
     */
    public void sendAlertToUser(String employeeId, AlertResponseDto responseDto) {
        try {
            if (isUserConnectedViaSse(employeeId)) {
                sendSseAlertToUser(employeeId, responseDto);
            } else {
                log.info("[앱 외부 감지, FCM 전송] employeeId = {}", employeeId);
                sendFcmNotification(employeeId, responseDto);
            }
        } catch (Exception e) {
            log.error("[알림 전송 실패] employeeId = {}, error = {}", employeeId, e.getMessage(), e);
            // throw new RuntimeException("알림 전송 중 오류 발생", e);
        }
    }

    private boolean isUserConnectedViaSse(String employeeId) {
        return emitters.containsKey(employeeId);
    }

    /**
     * 알림 SSE 전송
     *
     * @param emitterId
     * @param responseDto
    */
    public void sendSseAlertToUser(String emitterId, AlertResponseDto responseDto) {
        log.info("[알림 SSE 전송] 서비스 진입");

        SseEmitter emitter = emitters.get(emitterId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("SSE_ALERT")
                        .data(responseDto));
            } catch (IOException e) {
                emitters.remove(emitterId);
                log.error("[SSE 전송 실패, FCM으로 전환] employeeId = {}", emitterId, e);
                sendFcmNotification(emitterId, responseDto);
            } catch (Exception e) {
                log.error("[알림 SSE 전송 중 예기치 못한 오류] employeeId = {}", emitterId, e);
                // 필요시 추가 조치 가능
            }
        } else {
            log.warn("[알림 SSE 전송] Emitter가 존재하지 않습니다. employeeId = {}", emitterId);
        }

        log.info("[알림 SSE 전송] 처리 완료.");
    }

    /**
     * 알림 FCM 구현
     *
     * @param employeeId
     */
    public void sendFcmNotification(String employeeId, AlertResponseDto responseDto) {
        try {
            String token = redisTemplate.opsForValue().get("userfcm:" + employeeId);

            if (token == null) {
                log.warn("FCM 토큰이 존재하지 않습니다 : employeeId = {}", employeeId);
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("alertId", String.valueOf(responseDto.getAlertId()));
            data.put("id", responseDto.getId());
//            data.put("title", responseDto.getTitle());
//            data.put("message", responseDto.getMessage());
            data.put("kind", responseDto.getKind());
            data.put("isRead", String.valueOf(responseDto.isRead()));
            data.put("timestamp", responseDto.getTimestamp().toString());
            data.put("createdAt", responseDto.getCreatedAt().toString());

            // FCM 메시지 요청 생성
            FcmMessageRequestDto request = FcmMessageRequestDto.builder()
                    .title(responseDto.getTitle())
                    .body(responseDto.getMessage())
                    .token(token)
                    .data(data)
                    .build();

            // FCM 클라이언트로 메시지 전송
            firebaseClient.sendNotification(request);
            log.info("[FCM 전송 성공] employeeId = {}, token = {}", employeeId, token);

        } catch (DataAccessException e) {
            log.error("[FCM 전송 실패] Redis 접근 실패 : employeeId={}, error = {}", employeeId, e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.REDIS_ACCESS_FAILED);

        } catch (Exception e) {
            log.error("[FCM 전송 실패] 알 수 없는 오류 : employeeId = {}, error = {}", employeeId, e.getMessage(), e);
        }
    }

}
