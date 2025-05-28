package kr.ssok.ssom.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequestDto {
    
    /**
     * 현재 비밀번호
     */
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;
    
    /**
     * 새 비밀번호
     */
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 최소 8자리이며, 영문자, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다."
    )
    private String newPassword;
    
    /**
     * 새 비밀번호 확인
     */
    @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
    private String confirmPassword;
}
