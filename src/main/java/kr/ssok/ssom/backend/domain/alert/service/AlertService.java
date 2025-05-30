package kr.ssok.ssom.backend.domain.alert.service;

import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.alert.dto.*;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AlertService {
    SseEmitter subscribe(String username, String lastEventId, HttpServletResponse response);
    List<AlertResponseDto> getAllAlertsForUser(String employeeId);
    void modifyAlertStatus(AlertModifyRequestDto request);

    List<AlertResponseDto> createAlert(AlertRequestDto request, AlertKind kind);
    List<AlertResponseDto> createGrafanaAlert(AlertGrafanaRequestDto alertGrafanaRequestDto);
    List<AlertResponseDto> createOpensearchAlert(AlertOpensearchRequestDto alertOpensearchRequest);
    List<AlertResponseDto> createIssueAlert(AlertIssueRequestDto alertIssueRequest);
    //List<AlertResponseDto> createDevopsAlert(AlertSendRequestDto alertSendRequest);

    //void sendAlertToUsers(AlertSendRequestDto request);
    //List<AlertResponseDto> getAlertsByKind(Long userId, AlertKind kind);
}

