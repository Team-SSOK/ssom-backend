package kr.ssok.ssom.backend.domain.alert.dto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Tag(name = "AlertOpensearchRequestDto", description = "Opensearch에서 보내주는 포맷")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AlertOpensearchRequestDto {
    private String request;
}
