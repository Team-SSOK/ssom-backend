package kr.ssok.ssom.backend.domain.alert.service;

import kr.ssok.ssom.backend.domain.alert.dto.AlertModifyRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertResponseDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertSendRequestDto;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;

import java.util.List;

public interface AlertService {
    List<AlertResponseDto> createAlert(AlertRequestDto request, AlertKind kind);
    //void sendAlertToUsers(AlertSendRequestDto request);
    //List<AlertResponseDto> getAlertsByKind(Long userId, AlertKind kind);
    List<AlertResponseDto> getAllAlertsForUser(Long userId);
    void modifyAlertStatus(AlertModifyRequestDto request);
}

