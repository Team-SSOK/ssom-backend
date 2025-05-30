package kr.ssok.ssom.backend.domain.alert.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlertRequestDto {
    private String _index;
    private String title;
    private String message;

//    private String id;
//    private String level;
//    private String app;
//    private String timestamp;
//    private String message;
}
