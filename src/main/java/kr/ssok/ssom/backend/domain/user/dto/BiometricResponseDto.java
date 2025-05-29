package kr.ssok.ssom.backend.domain.user.dto;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BiometricResponseDto {

    private Boolean success;
    private String message;
    private Long biometricId;
    
    // 에러 응답용 필드들
    private String errorCode;
    private Integer remainingAttempts;
}