package kr.ssok.ssom.backend.domain.alert.service;

import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.alert.dto.*;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AlertService {
    SseEmitter subscribe(String username, String lastEventId, HttpServletResponse response);
    List<AlertResponseDto> getAllAlertsForUser(String employeeId);
    Page<AlertResponseDto> getPagedAlertsForUser(String employeeId, Pageable pageable);
    AlertResponseDto modifyAlertStatus(AlertModifyRequestDto request);
    List<AlertResponseDto> modifyAllAlertStatus(String employeeId);
    void deleteAlert(AlertModifyRequestDto request);

    // 기존 동기 처리 메서드들
    void createGrafanaAlert(AlertGrafanaRequestDto requestDto);
    void createOpensearchAlert(String requestStr);
    void createIssueAlert(AlertIssueRequestDto requestDto);
    void createDevopsAlert(AlertDevopsRequestDto requestDto);

    // 새로운 비동기 처리 메서드들
    void createGrafanaAlertAsync(AlertGrafanaRequestDto requestDto);
    void createOpensearchAlertAsync(String requestStr);
    void createIssueAlertAsync(AlertIssueRequestDto requestDto);
    void createDevopsAlertAsync(AlertDevopsRequestDto requestDto);

    void createAlert(AlertRequestDto request, AlertKind kind);
    void sendAlertToUser(String employeeId, AlertResponseDto alertResponseDto);
    void sendSseAlertToUser(String emitterId, AlertResponseDto alertResponseDto);
    void sendFcmNotification(String employeeId, AlertResponseDto alertResponseDto);
    
    // SSE 연결 관리 메서드
    void cleanupDisconnectedEmitters();
    int getActiveEmitterCount();
}

