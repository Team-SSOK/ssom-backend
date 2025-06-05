package kr.ssok.ssom.backend.domain.alert.dto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;

@Tag(name = "AlertSendRequestDto", description = "DevOps에서 보내주는 포맷")
@Getter
public class AlertDevopsRequestDto {
    private String level;
    private String app;
    private String timestamp;
    private String message;
}
