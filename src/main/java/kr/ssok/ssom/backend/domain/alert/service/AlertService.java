package kr.ssok.ssom.backend.domain.alert.service;

import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.alert.dto.AlertModifyRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertResponseDto;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AlertService {
    SseEmitter subscribe(String username, String lastEventId, HttpServletResponse response);
    List<AlertResponseDto> createAlert(AlertRequestDto request, AlertKind kind);
    //void sendAlertToUsers(AlertSendRequestDto request);
    //List<AlertResponseDto> getAlertsByKind(Long userId, AlertKind kind);
    List<AlertResponseDto> getAllAlertsForUser(String employeeId);
    void modifyAlertStatus(AlertModifyRequestDto request);
}

