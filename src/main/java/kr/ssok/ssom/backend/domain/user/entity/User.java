package kr.ssok.ssom.backend.domain.user.entity;

import jakarta.persistence.*;
import kr.ssok.ssom.backend.global.entity.TimeStamp;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends TimeStamp {
    @Id
    @Column(name = "employee_id", length = 10)
    private String id; // 사원번호 ("APP0001", "BANK0001" 등)

    @Column(name = "username", nullable = false, length = 50)
    private String username; // 이름

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false, length = 20)
    private Department department; // 부서

    @Column(name = "github_id", length = 50)
    private String githubId;
}
