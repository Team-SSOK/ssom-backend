package kr.ssok.ssom.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 검색 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 검색 결과")
public class UserSearchResponseDto {
    
    @Schema(description = "사원번호", example = "APP0001")
    private String employeeId;
    
    @Schema(description = "사용자명", example = "홍길동")
    private String username;
    
    @Schema(description = "부서", example = "APPLICATION")
    private Department department;
    
    @Schema(description = "GitHub ID 보유 여부", example = "true")
    private Boolean hasGithubId;
    
    @Schema(description = "GitHub ID", example = "github_user1")
    private String githubId;
    
    /**
     * User 엔티티로부터 DTO 생성
     * @param user User 엔티티
     * @return UserSearchResponseDto
     */
    public static UserSearchResponseDto from(User user) {
        return UserSearchResponseDto.builder()
                .employeeId(user.getId())
                .username(user.getUsername())
                .department(user.getDepartment())
                .hasGithubId(user.getGithubId() != null && !user.getGithubId().trim().isEmpty())
                .githubId(user.getGithubId())
                .build();
    }
}
