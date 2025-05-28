package kr.ssok.ssom.backend.domain.alert.dto;

import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import lombok.Getter;

@Getter
public class AlertSendRequestDto {
    private String title;
    private String message;
    private AlertKind kind;
}
