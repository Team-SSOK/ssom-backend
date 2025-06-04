package kr.ssok.ssom.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub Webhook Repository 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubWebhookRepositoryDto {
    
    /**
     * Repository ID
     */
    private Long id;
    
    /**
     * Repository 이름
     */
    private String name;
    
    /**
     * Repository 전체 이름 (owner/repo)
     */
    @JsonProperty("full_name")
    private String fullName;
    
    /**
     * Repository 소유자
     */
    private GitHubWebhookUserDto owner;
    
    /**
     * Repository 설명
     */
    private String description;
    
    /**
     * 비공개 Repository 여부
     */
    @JsonProperty("private")
    private Boolean isPrivate;
    
    /**
     * Repository HTML URL
     */
    @JsonProperty("html_url")
    private String htmlUrl;
    
    /**
     * Repository API URL
     */
    private String url;
    
    /**
     * Repository 기본 브랜치
     */
    @JsonProperty("default_branch")
    private String defaultBranch;
}
