package kr.ssok.ssom.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub Webhook 사용자 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubWebhookUserDto {
    
    /**
     * GitHub 사용자 ID
     */
    private Long id;
    
    /**
     * GitHub 사용자명 (로그인 ID)
     */
    private String login;
    
    /**
     * GitHub 사용자 표시명
     */
    private String name;
    
    /**
     * 사용자 타입 (User, Bot, etc.)
     */
    private String type;
    
    /**
     * 사이트 관리자 여부
     */
    @JsonProperty("site_admin")
    private Boolean siteAdmin;
    
    /**
     * 사용자 아바타 URL
     */
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    /**
     * 사용자 프로필 URL
     */
    @JsonProperty("html_url")
    private String htmlUrl;
    
    /**
     * 사용자 API URL
     */
    private String url;
}
