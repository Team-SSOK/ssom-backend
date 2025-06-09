package kr.ssok.ssom.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.ssok.ssom.backend.domain.user.entity.User;
import lombok.*;

/**
 * 사용자 목록 조회 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 목록 조회 응답")
public class UserListResponseDto {
    
    @Schema(description = "사원번호", example = "CHN0001")
    private String id;
    
    @Schema(description = "사용자명", example = "홍길동")
    private String username;
    
    @Schema(description = "부서", example = "CHANNEL")
    private String department;
    
    @Schema(description = "GitHub ID", example = "hong-gildong")
    private String githubId;
    
    /**
     * User 엔티티로부터 UserListResponseDto 생성
     */
    public static UserListResponseDto from(User user) {
        return UserListResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .department(user.getDepartment().name())
                .githubId(user.getGithubId())
                .build();
    }
}
