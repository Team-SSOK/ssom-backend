package kr.ssok.ssom.backend.domain.user.dto;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class LoginResponseDto {
    
    // 기존 필드들 (기존 로그인 API와 호환성 유지)
    private String accessToken;
    private String refreshToken;
    private String sseToken; // SSE 전용 토큰 추가
    
    // 생체인증 로그인을 위한 추가 필드들
    private String username;
    private String department;
    private Long expiresIn; // 토큰 만료 시간 (초)
    
    // 생체인증 관련 추가 정보
    private Boolean biometricEnabled;
    private String lastLoginAt;
}
