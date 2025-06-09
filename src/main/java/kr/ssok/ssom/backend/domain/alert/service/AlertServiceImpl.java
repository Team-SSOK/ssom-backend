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
import kr.ssok.ssom.backend.global.dto.GitHubIssueResponseDto;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

        // 3. 기존 emitter 존재 시 안전하게 제거
        cleanupExistingEmitter(emitterId);

        // 4. emitter 생성 및 등록
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(emitterId, emitter);

        log.info("[알림 SSE 구독] 연결 완료 : employeeId = {}, emitterId = {}", employeeId, emitterId);

        // 5. emitter 이벤트 콜백 등록 - Security Context 없이 안전하게 처리
        setupEmitterCallbacks(emitter, emitterId);

        // 6. 클라이언트에 연결 초기 이벤트 전송
        sendInitialEvent(emitter, emitterId);

        log.info("[알림 SSE 구독] SSE 연결 완료 : emitterId = {}", emitterId);
        return emitter;
    }

    /**
     * 기존 emitter 안전하게 정리
     */
    private void cleanupExistingEmitter(String emitterId) {
        SseEmitter existingEmitter = emitters.remove(emitterId);
        if (existingEmitter != null) {
            log.warn("[알림 SSE 구독] 기존 emitter 존재. 안전하게 제거 : emitterId = {}", emitterId);
            try {
                existingEmitter.complete();
            } catch (Exception e) {
                log.debug("[알림 SSE 구독] 기존 emitter 정리 중 예외 무시 : emitterId = {}, error = {}", 
                         emitterId, e.getMessage());
            }
        }
    }

    /**
     * Emitter 콜백 설정 - Security Context 없이 안전하게 처리
     */
    private void setupEmitterCallbacks(SseEmitter emitter, String emitterId) {
        // onCompletion: 정상 완료 시
        emitter.onCompletion(() -> {
            log.info("[Emitter 완료] emitterId = {}", emitterId);
            emitters.remove(emitterId);
        });

        // onTimeout: 타임아웃 시 - Security Context 사용 안함
        emitter.onTimeout(() -> {
            log.info("[Emitter 타임아웃] emitterId = {}", emitterId);
            emitters.remove(emitterId);
            // 명시적으로 complete 호출하지 않음 (이미 타임아웃됨)
        });

        // onError: 오류 발생 시 - Security Context 사용 안함
        emitter.onError((throwable) -> {
            log.error("[Emitter 오류 발생] emitterId = {}, error = {}", 
                     emitterId, throwable.getMessage());
            emitters.remove(emitterId);
            // 에러 발생 시에도 명시적 complete 불필요
        });
    }

    /**
     * 초기 연결 이벤트 전송
     */
    private void sendInitialEvent(SseEmitter emitter, String emitterId) {
        try {
            String eventId = createTimeIncludeId(emitterId);
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("SSE_ALERT_INIT")
                    .data("connected")
                    .reconnectTime(3000L)); // 재연결 시간 설정

            log.info("[알림 SSE 구독] SSE 초기 이벤트 전송 성공 : emitterId = {}, eventId = {}", emitterId, eventId);

        } catch (IOException e) {
            emitters.remove(emitterId);
            log.error("[알림 SSE 구독] 초기 이벤트 전송 실패 : emitterId = {}, error = {}", 
                     emitterId, e.getMessage());
            throw new BaseException(BaseResponseStatus.SSE_INIT_ERROR);
        } catch (IllegalStateException e) {
            emitters.remove(emitterId);
            log.error("[알림 SSE 구독] Emitter 상태 오류 : emitterId = {}, error = {}", 
                     emitterId, e.getMessage());
            throw new BaseException(BaseResponseStatus.SSE_INIT_ERROR);
        }
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
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
            List<AlertStatus> alertStatusList = alertStatusRepository.findByUser_IdAndAlert_CreatedAtAfter(employeeId, oneWeekAgo);

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
    @Transactional
    @Override
    public AlertResponseDto modifyAlertStatus(AlertModifyRequestDto request) {
        log.info("[알림 개별 상태 변경] 서비스 진입 : alertStatusId = {}, isRead = {}"
                , request.getAlertStatusId(), request.isRead());

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

            return AlertResponseDto.from(status);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[알림 개별 상태 변경] 처리 중 예외 발생 : alertStatusId = {}, error = {}", request.getAlertStatusId(), e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 알림 일괄 상태 변경
     *
     * @param employeeId
     */
    @Transactional
    @Override
    public List<AlertResponseDto> modifyAllAlertStatus(String employeeId) {
        log.info("[알림 일괄 상태 변경] 서비스 진입 : employeeId = {}", employeeId);

        // 1. 해당 유저의 읽지 않은 알림 조회
        List<AlertStatus> unreadAlerts = alertStatusRepository.findByUser_IdAndIsReadFalse(employeeId);

        // 2. 읽음 처리
        unreadAlerts.forEach(AlertStatus::markAsRead);

        // 3. 저장은 @Transactional이므로 flush 없이도 commit 시점에 처리됨

        // 4. 전체 알림 상태 다시 조회해서 응답 리스트 구성
        List<AlertStatus> allAlerts = alertStatusRepository.findByUser_IdOrderByAlert_CreatedAtDesc(employeeId);

        return allAlerts.stream()
                .map(AlertResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 알림 개별 삭제
     *
     *  @param request AlertModifyRequestDto
     */
    @Transactional
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
                return;
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
     * @param requestDto
     */
    @Override
    public void createIssueAlert(AlertIssueRequestDto requestDto) {
        log.info("[Github 이슈 알림] 서비스 진입 : requestDto = {}", requestDto);

        try {
            // 0. 'ssom' 또는 'SSOM' 라벨이 있는지 확인
            boolean hasSsomLabel = requestDto.getIssue().getLabels().stream()
                    .anyMatch(label -> label.getName().equalsIgnoreCase("ssom"));

            if (!hasSsomLabel) {
                log.info("[Github 이슈 알림] 'ssom' 라벨이 없어 알림을 생성하지 않음");
                return;
            }

            // 1. action 에 따라 title 설정
            String alertTitle;
            switch (requestDto.getAction()) {
                case "opened":
                    alertTitle = "[ISSUE] Opened";
                    break;
                case "reopened":
                    alertTitle = "[ISSUE] Reopened";
                    break;
                case "closed":
                    alertTitle = "[ISSUE] Closed";
                    break;
                default:
                    log.warn("[Github 이슈 알림] 알림 전송으로 지원하지 않는 action 값: {}", requestDto.getAction());
                    return;
            }

            // 2. Alert 저장
            Alert alert = Alert.builder()
                    .id(AlertKind.ISSUE + "_noNeedId")
                    .title(alertTitle)
                    .message("Github 이슈가 공유되었습니다.")
                    .kind(AlertKind.ISSUE)
                    .timestamp(requestDto.getIssue().getCreatedAt())
                    .build();
            alertRepository.save(alert);

            // 3. 알림 대상자 조회
            List<String> sharedLogins = requestDto.getIssue().getAssignees().stream()
                    .map(AlertIssueRequestDto.Assignee::getLogin)
                    .collect(Collectors.toList());

            if (sharedLogins.isEmpty()) {
                log.warn("[Github 이슈 알림] 공유 대상자가 지정되지 않음");
                return;
            }

            List<User> targetUsers = userRepository.findAllByGithubIdIn(sharedLogins);
            if (targetUsers.isEmpty()) {
                log.warn("[Github 이슈 알림] 공유 대상자가 존재하지 않음: {}", sharedLogins);
                throw new BaseException(BaseResponseStatus.ALERT_TARGET_USER_NOT_FOUND);
            }

            // 4. AlertStatus 생성 및 전송
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

            log.info("[Github 이슈 알림] 서비스 처리 완료");
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Github 이슈 알림] 처리 중 예외 발생", e);
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
    @Transactional
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
     * 알림 SSE 전송 - 안전한 전송 및 예외 처리
     *
     * @param emitterId
     * @param responseDto
    */
    public void sendSseAlertToUser(String emitterId, AlertResponseDto responseDto) {
        log.info("[알림 SSE 전송] 서비스 진입 - emitterId: {}", emitterId);

        SseEmitter emitter = emitters.get(emitterId);
        if (emitter == null) {
            log.warn("[알림 SSE 전송] Emitter가 존재하지 않습니다. employeeId = {}", emitterId);
            return;
        }

        try {
            // Thread-safe SSE 전송
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .name("SSE_ALERT")
                        .id(createTimeIncludeId(emitterId))
                        .data(responseDto)
                        .reconnectTime(3000L));
                        
                log.info("[알림 SSE 전송] 성공 - emitterId: {}, alertId: {}", emitterId, responseDto.getAlertId());
            }
        } catch (IOException e) {
            log.error("[SSE 전송 실패, FCM으로 전환] employeeId = {}, error = {}", emitterId, e.getMessage());
            handleSseFailure(emitterId, responseDto);
        } catch (IllegalStateException e) {
            log.error("[SSE 전송 실패 - Emitter 상태 오류] employeeId = {}, error = {}", emitterId, e.getMessage());
            handleSseFailure(emitterId, responseDto);
        } catch (Exception e) {
            log.error("[알림 SSE 전송 중 예기치 못한 오류] employeeId = {}, error = {}", emitterId, e.getMessage(), e);
            handleSseFailure(emitterId, responseDto);
        }

        log.debug("[알림 SSE 전송] 처리 완료 - emitterId: {}", emitterId);
    }

    /**
     * SSE 전송 실패 시 처리
     */
    private void handleSseFailure(String emitterId, AlertResponseDto responseDto) {
        // 실패한 emitter 제거
        emitters.remove(emitterId);
        
        // FCM으로 대체 전송 시도
        try {
            sendFcmNotification(emitterId, responseDto);
            log.info("[FCM 대체 전송] 성공 - employeeId = {}", emitterId);
        } catch (Exception fcmException) {
            log.error("[FCM 대체 전송 실패] employeeId = {}, error = {}", emitterId, fcmException.getMessage());
        }
    }

    /**
     * SSE 연결 상태 확인 및 정리
     */
    public void cleanupDisconnectedEmitters() {
        emitters.entrySet().removeIf(entry -> {
            String emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            
            try {
                // 간단한 heartbeat 전송으로 연결 상태 확인
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
                return false; // 연결 유지
            } catch (Exception e) {
                log.info("[SSE 정리] 비활성 emitter 제거 - emitterId: {}", emitterId);
                return true; // 연결 끊어짐, 제거
            }
        });
    }

    /**
     * 현재 활성 SSE 연결 수 반환
     */
    public int getActiveEmitterCount() {
        return emitters.size();
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
