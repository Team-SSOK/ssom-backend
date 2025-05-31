package kr.ssok.ssom.backend.domain.issue.repository;

import kr.ssok.ssom.backend.domain.issue.entity.Issue;
import kr.ssok.ssom.backend.domain.issue.entity.constant.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    
    /**
     * 특정 사용자가 생성한 Issue 목록 조회
     * @param createdByEmployeeId 생성자 사원번호
     * @return Issue 목록
     */
    List<Issue> findByCreatedByEmployeeIdOrderByCreatedAtDesc(String createdByEmployeeId);
    
    /**
     * 특정 상태의 Issue 목록 조회
     * @param status Issue 상태
     * @return Issue 목록
     */
    List<Issue> findByStatusOrderByCreatedAtDesc(IssueStatus status);
    
    /**
     * GitHub Issue 번호로 Issue 조회
     * @param githubIssueNumber GitHub Issue 번호
     * @return Issue 엔티티
     */
    Optional<Issue> findByGithubIssueNumber(Long githubIssueNumber);
    
    /**
     * GitHub와 연동된 Issue 목록 조회
     * @return GitHub 연동된 Issue 목록
     */
    @Query("SELECT i FROM Issue i WHERE i.githubIssueNumber IS NOT NULL ORDER BY i.createdAt DESC")
    List<Issue> findGithubSyncedIssues();
    
    /**
     * GitHub와 연동되지 않은 Issue 목록 조회
     * @return GitHub 연동되지 않은 Issue 목록
     */
    @Query("SELECT i FROM Issue i WHERE i.githubIssueNumber IS NULL ORDER BY i.createdAt DESC")
    List<Issue> findGithubUnsyncedIssues();
    
    /**
     * 특정 사용자가 생성한 특정 상태의 Issue 목록 조회
     * @param createdByEmployeeId 생성자 사원번호
     * @param status Issue 상태
     * @return Issue 목록
     */
    List<Issue> findByCreatedByEmployeeIdAndStatusOrderByCreatedAtDesc(
            String createdByEmployeeId, 
            IssueStatus status
    );
    
    /**
     * 특정 담당자가 할당된 Issue 목록 조회
     * @param githubId 담당자 GitHub ID
     * @return Issue 목록
     */
    @Query("SELECT i FROM Issue i JOIN i.assigneeGithubIds a WHERE a = :githubId ORDER BY i.createdAt DESC")
    List<Issue> findByAssigneeGithubId(@Param("githubId") String githubId);
    
    /**
     * 특정 로그 ID가 포함된 Issue 목록 조회
     * @param logId 로그 ID
     * @return Issue 목록
     */
    @Query("SELECT i FROM Issue i JOIN i.logIds l WHERE l = :logId ORDER BY i.createdAt DESC")
    List<Issue> findByLogId(@Param("logId") String logId);
    
    /**
     * 전체 Issue 목록 조회 (최신순)
     * @return 전체 Issue 목록
     */
    List<Issue> findAllByOrderByCreatedAtDesc();
}
