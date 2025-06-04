package kr.ssok.ssom.backend.domain.alert.dto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "AlertGrafanaRequestDto", description = "그라파나에서 보내주는 포맷")
@Getter
@Builder
public class AlertGrafanaRequestDto {
    private List<AlertRequestDto> alerts;
}
