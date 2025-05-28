package kr.ssok.ssom.backend.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDto {

    @NotNull(message = "사원번호가 null 입니다")
    private String employeeId;

    @NotNull(message = "비밀번호를 입력해 주세요.")
    private String password;
}
