package kr.ssok.ssom.backend.domain.issue.entity;

import jakarta.persistence.*;
import kr.ssok.ssom.backend.domain.issue.entity.constant.IssueStatus;
import kr.ssok.ssom.backend.global.entity.TimeStamp;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "issues")
public class Issue extends TimeStamp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long issueId;
    
    @Column(name = "github_issue_number")
    private Long githubIssueNumber;      // GitHub Issue 번호 (null이면 GitHub 연동 실패)
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private IssueStatus status = IssueStatus.OPEN;  // OPEN, CLOSED
    
    // 사용자 정보
    @Column(name = "created_by_employee_id", nullable = false, length = 10)
    private String createdByEmployeeId;  // 생성자 사원번호 (User.id)
    
    // 다중 담당자 지원
    @ElementCollection
    @CollectionTable(
        name = "issue_assignees", 
        joinColumns = @JoinColumn(name = "issue_id")
    )
    @Column(name = "github_id", length = 50)
    @Builder.Default
    private List<String> assigneeGithubIds = new ArrayList<>();  // 담당자들의 GitHub ID 목록
    
    // 로그 정보
    @ElementCollection
    @CollectionTable(
        name = "issue_log_ids", 
        joinColumns = @JoinColumn(name = "issue_id")
    )
    @Column(name = "log_id", length = 100)
    @Builder.Default
    private List<String> logIds = new ArrayList<>();         // Opensearch 로그 ID들
    
    /**
     * GitHub 연동 상태 확인
     * @return GitHub Issue 번호가 있으면 true, 없으면 false
     */
    public boolean isGithubSynced() {
        return this.githubIssueNumber != null;
    }
    
    /**
     * 담당자 추가
     * @param githubId 추가할 담당자의 GitHub ID
     */
    public void addAssignee(String githubId) {
        if (githubId != null && !githubId.trim().isEmpty() && !this.assigneeGithubIds.contains(githubId)) {
            this.assigneeGithubIds.add(githubId);
        }
    }
    
    /**
     * 로그 ID 추가
     * @param logId 추가할 로그 ID
     */
    public void addLogId(String logId) {
        if (logId != null && !logId.trim().isEmpty() && !this.logIds.contains(logId)) {
            this.logIds.add(logId);
        }
    }
    
    /**
     * Issue 상태 변경
     * @param status 변경할 상태
     */
    public void updateStatus(IssueStatus status) {
        this.status = status;
    }
    
    /**
     * GitHub Issue 정보 설정
     * @param githubIssueNumber GitHub Issue 번호
     */
    public void setGithubInfo(Long githubIssueNumber) {
        this.githubIssueNumber = githubIssueNumber;
    }
}
