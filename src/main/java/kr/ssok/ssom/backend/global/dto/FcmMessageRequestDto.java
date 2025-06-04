package kr.ssok.ssom.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * FCM 알림 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmMessageRequestDto {
    private String title;   // 알림 제목 (예: "")
    private String body;    // 알림 내용 (예: "")
    private String token;   // FCM 토큰
}
