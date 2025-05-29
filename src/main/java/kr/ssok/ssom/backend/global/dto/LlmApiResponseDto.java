package kr.ssok.ssom.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class LlmApiResponseDto<T> {
    private boolean isSuccess;
    private String code;
    private String message;
    private List<T> result;
}
