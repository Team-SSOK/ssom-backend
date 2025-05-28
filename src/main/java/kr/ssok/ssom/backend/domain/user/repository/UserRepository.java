package kr.ssok.ssom.backend.domain.user.repository;

import kr.ssok.ssom.backend.domain.user.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    // 전화번호 중복 체크
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * 특정 부서 prefix로 시작하는 사원번호 중에서 가장 최근(마지막) 번호를 조회
     * @param pattern 부서 prefix + "%" (예: "CHN%", "CORE%")
     * @return 해당 부서의 마지막 사원번호 (예: "CHN0005")
     */
    @Query("SELECT u.id FROM User u WHERE u.id LIKE :pattern ORDER BY u.id DESC")
    List<String> findEmployeeIdsByPrefix(@Param("pattern") String pattern, Pageable pageable);

    /**
     * 부서별 마지막 사원번호 조회 (헬퍼 메서드)
     * @param pattern 부서 prefix + "%"
     * @return 마지막 사원번호 또는 null (해당 부서 첫 번째 사원인 경우)
     */
    default String findLastEmployeeIdByPrefix(String pattern) {
        List<String> ids = findEmployeeIdsByPrefix(pattern, PageRequest.of(0, 1));
        return ids.isEmpty() ? null : ids.get(0);
    }
}
