package kr.ssok.ssom.backend.domain.alert.dto.fcm;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 토큰 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
public class FcmRegisterRequestDto {
    private String token;    // FCM 토큰
}
