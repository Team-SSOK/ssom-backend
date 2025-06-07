package kr.ssok.ssom.backend.domain.alert.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AlertModifyRequestDto {
    private Long alertStatusId;

    @JsonProperty("isRead")
    private boolean isRead;
}

