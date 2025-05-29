package kr.ssok.ssom.backend.domain.user.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BiometricStatusDto {

    private Boolean isRegistered;
    private List<String> availableTypes;
    private Integer deviceCount;
    private LocalDateTime lastUsedAt;
}