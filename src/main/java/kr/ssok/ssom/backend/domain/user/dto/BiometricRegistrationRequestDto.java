package kr.ssok.ssom.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BiometricRegistrationRequestDto {

//    @NotBlank(message = "사원번호는 필수입니다.")
//    @Size(min = 4, max = 10, message = "사원번호는 4-10자 사이여야 합니다.")
//    private String employeeId;

    @NotBlank(message = "생체인증 타입은 필수입니다.")
    @Pattern(regexp = "FINGERPRINT|FACE", message = "유효하지 않은 생체인증 타입입니다.")
    private String biometricType;

    @NotBlank(message = "디바이스 ID는 필수입니다.")
    @Size(min = 10, max = 255, message = "디바이스 ID는 10-255자 사이여야 합니다.")
    private String deviceId;

    @NotBlank(message = "생체인증 해시는 필수입니다.")
    @Size(max = 500, message = "생체인증 해시는 500자를 초과할 수 없습니다.")
    private String biometricHash;

    @Size(max = 1000, message = "디바이스 정보는 1000자를 초과할 수 없습니다.")
    private String deviceInfo;
}