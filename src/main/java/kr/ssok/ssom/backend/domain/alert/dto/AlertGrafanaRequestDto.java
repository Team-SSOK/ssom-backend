package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AlertGrafanaRequestDto {
    private List<AlertRequestDto> alerts;
}
