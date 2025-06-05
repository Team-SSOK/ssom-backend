package kr.ssok.ssom.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * FCM 알림 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmMessageRequestDto {
    private String title;   // 알림 제목 (예: "[ERROR] ssok-bank")
    private String body;    // 알림 내용 (예: "uthentication error: Authorization header is missing or invalid")
    private String token;   // FCM 토큰
    private Map<String, String> data;
}
