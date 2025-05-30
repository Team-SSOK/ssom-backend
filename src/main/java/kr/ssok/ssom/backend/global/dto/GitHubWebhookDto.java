package kr.ssok.ssom.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub Webhook 이벤트 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubWebhookDto {
    
    /**
     * Webhook 이벤트 액션 (opened, closed, edited, etc.)
     */
    private String action;
    
    /**
     * Issue 정보
     */
    private GitHubWebhookIssueDto issue;
    
    /**
     * Repository 정보
     */
    private GitHubWebhookRepositoryDto repository;
    
    /**
     * 이벤트 발신자 정보
     */
    private GitHubWebhookSenderDto sender;
    
    /**
     * 조직 정보 (선택사항)
     */
    private GitHubWebhookOrganizationDto organization;
}
