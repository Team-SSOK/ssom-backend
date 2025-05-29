package kr.ssok.ssom.backend.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BiometricLoginRequestDto {

    @NotBlank(message = "사원번호는 필수입니다.")
    @Size(min = 4, max = 10, message = "사원번호는 4-10자 사이여야 합니다.")
    private String employeeId;

    @NotBlank(message = "생체인증 타입은 필수입니다.")
    @Pattern(regexp = "FINGERPRINT|FACE|VOICE", message = "유효하지 않은 생체인증 타입입니다.")
    private String biometricType;

    @NotBlank(message = "디바이스 ID는 필수입니다.")
    private String deviceId;

    @NotBlank(message = "생체인증 해시는 필수입니다.")
    private String biometricHash;

    @NotNull(message = "타임스탬프는 필수입니다.")
    @Min(value = 1000000000000L, message = "유효하지 않은 타임스탬프입니다.")
    private Long timestamp;

    // 추가 보안 필드들 (향후 확장용)
    private String challengeResponse;
    private String deviceFingerprint;
}