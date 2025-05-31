package kr.ssok.ssom.backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GitHub API 설정
 */
@Configuration
@ConfigurationProperties(prefix = "github")
@Getter
@Setter
public class GitHubConfig {
    
    /**
     * GitHub API 관련 설정
     */
    private Api api = new Api();
    
    /**
     * GitHub Webhook 관련 설정
     */
    private Webhook webhook = new Webhook();
    
    @Getter
    @Setter
    public static class Api {
        /**
         * GitHub Personal Access Token
         */
        private String token;
        
        /**
         * Repository 소유자 (조직명 또는 사용자명)
         */
        private String owner;
        
        /**
         * Repository 이름
         */
        private String repository;
    }
    
    @Getter
    @Setter
    public static class Webhook {
        /**
         * GitHub Webhook Secret
         */
        private String secret;
    }
    
    /**
     * Authorization 헤더 값 생성
     * @return "token {personal_access_token}" 형식
     */
    public String getAuthorizationHeader() {
        if (api.token == null || api.token.trim().isEmpty()) {
            throw new IllegalStateException("GitHub API token이 설정되지 않았습니다. GITHUB_TOKEN 환경변수를 확인해주세요.");
        }
        return "token " + api.token.trim();
    }
    
    /**
     * Repository 전체 이름 반환
     * @return "owner/repository" 형식
     */
    public String getFullRepositoryName() {
        return api.owner + "/" + api.repository;
    }
    
    /**
     * GitHub Issue URL 생성
     * @param issueNumber Issue 번호
     * @return GitHub Issue URL
     */
    public String getIssueUrl(Long issueNumber) {
        return String.format("https://github.com/%s/%s/issues/%d", 
                api.owner, api.repository, issueNumber);
    }
}
