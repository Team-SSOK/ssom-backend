package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Getter;

@Getter
public class AlertRequestDto {
    private String _index;
    private String title;
    private String message;
}
