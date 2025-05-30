package kr.ssok.ssom.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub Webhook Label 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubWebhookLabelDto {
    
    /**
     * Label ID
     */
    private Long id;
    
    /**
     * Label 이름
     */
    private String name;
    
    /**
     * Label 색상 (hex 코드)
     */
    private String color;
    
    /**
     * Label 설명
     */
    private String description;
    
    /**
     * 기본 Label 여부
     */
    private Boolean defaultLabel;
    
    /**
     * Label URL
     */
    private String url;
}
