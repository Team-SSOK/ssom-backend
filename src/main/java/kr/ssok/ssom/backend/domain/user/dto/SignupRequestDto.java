package kr.ssok.ssom.backend.domain.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequestDto {
    private String username;
    private String password;
    private String phoneNumber;
    private int departmentCode;
    private String githubId;
}
