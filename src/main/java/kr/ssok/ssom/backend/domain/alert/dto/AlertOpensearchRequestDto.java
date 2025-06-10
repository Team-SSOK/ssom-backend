package kr.ssok.ssom.backend.domain.alert.dto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Tag(name = "AlertOpensearchRequestDto", description = "Opensearch에서 보내주는 포맷")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AlertOpensearchRequestDto {
    private String id;          // 예 : 5x7xHpcBfhJZWUSwpfCE
    private String level;       // 예 : ERROR     -> title
    private String app;         // 예 : ssok-bank -> title
    private String timestamp;   // 예 : 2025-05-30T08:37:50.772492854+00:00
    private String message;     // 예 : Authentication error: Authorization header is missing or invalid
}
