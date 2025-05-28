package kr.ssok.ssom.backend.domain.user.security.principal;

import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spring Security Authentication에서 사용할 사용자 정보 클래스
 * @AuthenticationPrincipal 어노테이션으로 Controller에서 주입받을 수 있음
 */
@Getter
@AllArgsConstructor
public class UserPrincipal {
    
    /**
     * 사원번호 (PK)
     */
    private final String employeeId;
    
    /**
     * 사용자 이름
     */
    private final String username;
    
    /**
     * 부서 정보
     */
    private final Department department;
    
    /**
     * 전화번호
     */
    private final String phoneNumber;
    
    /**
     * GitHub ID
     */
    private final String githubId;
    
    /**
     * User 엔티티에서 UserPrincipal 생성
     * 
     * @param user User 엔티티
     * @return UserPrincipal 객체
     */
    public static UserPrincipal from(User user) {
        return new UserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getDepartment(),
            user.getPhoneNumber(),
            user.getGithubId()
        );
    }
    
    /**
     * 부서 코드 반환
     */
    public int getDepartmentCode() {
        return department.getCode();
    }
    
    /**
     * 부서명 반환
     */
    public String getDepartmentName() {
        return department.name();
    }
}
