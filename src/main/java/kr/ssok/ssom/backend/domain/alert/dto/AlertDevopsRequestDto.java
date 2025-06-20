package kr.ssok.ssom.backend.domain.alert.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Tag(name = "AlertSendRequestDto", description = "DevOps에서 보내주는 포맷")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDevopsRequestDto {
    private String level;
    private String app;
    private String timestamp;
    private String message;
}
