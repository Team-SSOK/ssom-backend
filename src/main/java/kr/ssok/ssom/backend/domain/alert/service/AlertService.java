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
    void deleteAlert(AlertModifyRequestDto request);

    void createAlert(AlertRequestDto request, AlertKind kind);
    void createGrafanaAlert(AlertGrafanaRequestDto requestDto);
    void createOpensearchAlert(AlertOpensearchRequestDto requestDto);
    void createIssueAlert(AlertIssueRequestDto requestDto);
    void createDevopsAlert(AlertSendRequestDto requestDto);
}

