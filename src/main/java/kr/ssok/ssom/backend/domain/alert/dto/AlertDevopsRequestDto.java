package kr.ssok.ssom.backend.domain.alert.dto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Tag(name = "AlertSendRequestDto", description = "DevOps에서 보내주는 포맷")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AlertDevopsRequestDto {
    private String level;
    private String app;
    private OffsetDateTime timestamp;
    private String message;
}
