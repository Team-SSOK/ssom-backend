package kr.ssok.ssom.backend.domain.user.repository;

import kr.ssok.ssom.backend.domain.user.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * User 엔티티의 Repository
 * PK가 String 타입 (사원번호)이므로 JpaRepository<User, String> 사용
 */
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * 전화번호 중복 체크
     * @param phoneNumber 전화번호
     * @return 존재 여부
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * 특정 부서 prefix로 시작하는 사원번호 중에서 가장 최근(마지막) 번호를 조회
     * @param pattern 부서 prefix + "%" (예: "CHN%", "CORE%")
     * @return 해당 부서의 마지막 사원번호들 (내림차순)
     */
    @Query("SELECT u.id FROM User u WHERE u.id LIKE :pattern ORDER BY u.id DESC")
    List<String> findEmployeeIdsByPrefix(@Param("pattern") String pattern, Pageable pageable);

    /**
     * 부서별 마지막 사원번호 조회 (헬퍼 메서드)
     * @param pattern 부서 prefix + "%" (예: "CHN%")
     * @return 마지막 사원번호 또는 null (해당 부서 첫 번째 사원인 경우)
     */
    default String findLastEmployeeIdByPrefix(String pattern) {
        List<String> ids = findEmployeeIdsByPrefix(pattern, PageRequest.of(0, 1));
        return ids.isEmpty() ? null : ids.get(0);
    }

    /**
     * 사원번호로 사용자 조회 (명시적 메서드)
     * 실제로는 findById와 동일하지만 더 명확한 의미 전달
     * @param employeeId 사원번호 (예: "CHN0001", "CORE0002")
     * @return User 엔티티 Optional
     */
    default Optional<User> findByEmployeeId(String employeeId) {
        return findById(employeeId); // PK가 사원번호이므로 findById와 동일
    }
    
    /**
     * 사원번호 존재 여부 확인
     * @param employeeId 사원번호
     * @return 존재 여부
     */
    default boolean existsByEmployeeId(String employeeId) {
        return existsById(employeeId); // PK 존재 여부 확인
    }
}
