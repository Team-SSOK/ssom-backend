package kr.ssok.ssom.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GitHub Webhook Issue 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubWebhookIssueDto {
    
    /**
     * Issue ID (GitHub 내부 ID)
     */
    private Long id;
    
    /**
     * Issue 번호 (Repository 내에서의 번호)
     */
    private Long number;
    
    /**
     * Issue 제목
     */
    private String title;
    
    /**
     * Issue 본문
     */
    private String body;
    
    /**
     * Issue 상태 (open, closed)
     */
    private String state;
    
    /**
     * Issue가 잠겨있는지 여부
     */
    private Boolean locked;
    
    /**
     * Issue 담당자 목록
     */
    private List<GitHubWebhookUserDto> assignees;
    
    /**
     * Issue 라벨 목록
     */
    private List<GitHubWebhookLabelDto> labels;
    
    /**
     * Issue 생성자
     */
    private GitHubWebhookUserDto user;
    
    /**
     * Issue 생성 시간
     */
    @JsonProperty("created_at")
    private String createdAt;
    
    /**
     * Issue 수정 시간
     */
    @JsonProperty("updated_at")
    private String updatedAt;
    
    /**
     * Issue 종료 시간 (closed 상태인 경우)
     */
    @JsonProperty("closed_at")
    private String closedAt;
    
    /**
     * Issue HTML URL
     */
    @JsonProperty("html_url")
    private String htmlUrl;
    
    /**
     * Issue API URL
     */
    private String url;
}
