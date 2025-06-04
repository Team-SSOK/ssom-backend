package kr.ssok.ssom.backend.domain.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    
    /**
     * 사원번호
     */
    private String employeeId;
    
    /**
     * 사용자 이름
     */
    private String username;
    
    /**
     * 전화번호
     */
    private String phoneNumber;
    
    /**
     * 부서명 (문자열)
     */
    private String department;
    
    /**
     * 부서 코드 (숫자)
     */
    private int departmentCode;
    
    /**
     * GitHub ID
     */
    private String githubId;
}
