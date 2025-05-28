package kr.ssok.ssom.backend.domain.alert.service;

import jakarta.transaction.Transactional;
import kr.ssok.ssom.backend.domain.alert.dto.AlertModifyRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertResponseDto;
import kr.ssok.ssom.backend.domain.alert.entity.Alert;
import kr.ssok.ssom.backend.domain.alert.entity.AlertStatus;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import kr.ssok.ssom.backend.domain.alert.repository.AlertRepository;
import kr.ssok.ssom.backend.domain.alert.repository.AlertStatusRepository;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final AlertRepository alertRepository;
    private final AlertStatusRepository alertStatusRepository;
    private final UserRepository userRepository;

    /*
    * 알림 SSE 구독
    * */
    public SseEmitter subscribe(Long userId) {
        log.info("[알림 SSE 구독] 서비스 진입");
        
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /*
     * 알림 SSE 전송
     * */
    public void sendAlertToUser(Long userId, AlertResponseDto alertResponseDto) {
        log.info("[알림 SSE 전송] 서비스 진입");
        
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("alert")
                        .data(alertResponseDto));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }
    
    /*
    * 알림 저장 및 전송
    * */
    @Override
    public List<AlertResponseDto> createAlert(AlertRequestDto request, AlertKind kind) {
        // 1. Alert 저장
        Alert alert = Alert.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .kind(kind)
                .build();
        alertRepository.save(alert);

        // 2. 전체 사용자 가져오기
        List<User> users = userRepository.findAll(); // 가정: 전체 사용자에게 알림 전송

        // 3. 각 사용자에게 AlertStatus 생성
        List<AlertStatus> statusList = new ArrayList<>();
        for (User user : users) {
            AlertStatus status = AlertStatus.builder()
                    .alert(alert)
                    .user(user)
                    .isRead(false)
                    .build();
            statusList.add(status);
        }
        alertStatusRepository.saveAll(statusList);

        // 4. DTO로 변환 후 반환
        List<AlertResponseDto> dtoList = statusList.stream()
                .map(AlertResponseDto::from)
                .collect(Collectors.toList());

        // 5. 알림 푸시
        for (AlertResponseDto dto : dtoList) {
            sendAlertToUser(dto.getUserId(), dto);
        }

        return dtoList;
    }

    /*
    @Override
    public void sendAlertToUsers(AlertSendRequestDto request) {
        Alert alert = Alert.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .kind(request.getKind())
                .build();
        alertRepository.save(alert);

        List<User> users = userRepository.findAll();
        for (User user : users) {
            AlertStatus status = new AlertStatus(null, user, alert, false, null);
            alertStatusRepository.save(status);
        }
    }
    */

    /*
    @Override
    public List<AlertResponseDto> getAlertsByKind(Long userId, AlertKind kind) {
        return alertStatusRepository.findByUserIdAndAlertKind(userId, kind)
                .stream()
                .map(AlertResponseDto::from)
                .collect(Collectors.toList());
    }
    */

    /*
    * 전체 알림 목록 조회
    * */
    @Override
    public List<AlertResponseDto> getAllAlertsForUser(Long userId) {
        log.info("[전체 알림 목록 조회] 서비스 진입");

        return alertStatusRepository.findByUserId(userId)
                .stream()
                .map(AlertResponseDto::from)
                .collect(Collectors.toList());
    }

    /*
     * 알림 상태 변경
     * */
    @Override
    public void modifyAlertStatus(AlertModifyRequestDto request) {
        log.info("[알림 상태 변경] 서비스 진입");

        AlertStatus status = alertStatusRepository.findById(request.getAlertStatusId())
                .orElseThrow(() -> new RuntimeException("AlertStatus not found"));
        if (request.isRead()) {
            status.markAsRead();
        } else {
            status.markAsUnread();
        }
    }
}
