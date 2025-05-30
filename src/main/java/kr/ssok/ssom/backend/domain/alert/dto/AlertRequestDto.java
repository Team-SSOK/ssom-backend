package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlertRequestDto {
    private String id;          // 예 : 5x7xHpcBfhJZWUSwpfCE
    private String level;       // 예 : ERROR
    private String app;         // 예 : ssok-bank
    private String timestamp;   // 예 : 2025-05-30T08:37:50.772492854+00:00
    private String message;     // 예 : Authentication error: Authorization header is missing or invalid
}
