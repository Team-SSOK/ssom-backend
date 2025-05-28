package kr.ssok.ssom.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequestDto {
    @NotBlank(message = "Refresh Token이 비어있습니다.")
    private String refreshToken;
}
