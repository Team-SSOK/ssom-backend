package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlertRequestDto {
    private String _index;
    private String title;
    private String message;
}
