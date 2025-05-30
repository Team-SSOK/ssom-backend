package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Getter;

@Getter
public class AlertOpensearchDto {
    private String id;
    private String level;
    private String app;
    private String timestamp;
    private String message;
}
