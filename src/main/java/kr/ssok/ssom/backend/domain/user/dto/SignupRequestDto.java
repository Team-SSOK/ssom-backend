package kr.ssok.ssom.backend.domain.user.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequestDto {
    @NotBlank(message = "사용자명은 필수입니다")
    private String username;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
    
    @NotBlank(message = "전화번호는 필수입니다")
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @Min(value = 1, message = "부서코드는 1 이상이어야 합니다")
    @Max(value = 4, message = "부서코드는 4 이하여야 합니다")
    @JsonProperty("departmentCode")
    private int departmentCode;
    
    @JsonProperty("githubId")
    private String githubId;
}
