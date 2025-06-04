package kr.ssok.ssom.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub Webhook Organization 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubWebhookOrganizationDto {
    
    /**
     * Organization ID
     */
    private Long id;
    
    /**
     * Organization 로그인 ID
     */
    private String login;
    
    /**
     * Organization 표시명
     */
    private String name;
    
    /**
     * Organization 설명
     */
    private String description;
    
    /**
     * Organization 아바타 URL
     */
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    /**
     * Organization HTML URL
     */
    @JsonProperty("html_url")
    private String htmlUrl;
    
    /**
     * Organization API URL
     */
    private String url;
}
