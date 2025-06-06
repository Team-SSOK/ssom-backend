package kr.ssok.ssom.backend.domain.alert.service;

import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.alert.dto.*;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import kr.ssok.ssom.backend.global.dto.GitHubIssueResponseDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AlertService {
    SseEmitter subscribe(String username, String lastEventId, HttpServletResponse response);
    List<AlertResponseDto> getAllAlertsForUser(String employeeId);
    AlertResponseDto modifyAlertStatus(AlertModifyRequestDto request);
    void deleteAlert(AlertModifyRequestDto request);

    void createGrafanaAlert(AlertGrafanaRequestDto requestDto);
    void createOpensearchAlert(String requestStr);
    void createIssueAlert(AlertIssueRequestDto requestDto);
    void createDevopsAlert(AlertDevopsRequestDto requestDto);

    void createAlert(AlertRequestDto request, AlertKind kind);
    void sendAlertToUser(String employeeId, AlertResponseDto alertResponseDto);
    void sendSseAlertToUser(String emitterId, AlertResponseDto alertResponseDto);
    void sendFcmNotification(String employeeId, AlertResponseDto alertResponseDto);
    
    // SSE 연결 관리 메서드
    void cleanupDisconnectedEmitters();
    int getActiveEmitterCount();
}

