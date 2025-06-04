package kr.ssok.ssom.backend.domain.alert.dto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;

@Tag(name = "AlertOpensearchRequestDto", description = "Opensearch에서 보내주는 포맷")
@Getter
public class AlertOpensearchRequestDto {
    private String request;
}
