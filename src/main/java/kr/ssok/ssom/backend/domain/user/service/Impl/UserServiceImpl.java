package kr.ssok.ssom.backend.domain.user.service.Impl;

import kr.ssok.ssom.backend.domain.user.dto.SignupRequestDto;
import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.repository.UserRepository;
import kr.ssok.ssom.backend.domain.user.service.UserService;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void registerUser(SignupRequestDto requestDto) {
        // 중복 가입 확인
        if (userRepository.existsByPhoneNumber(requestDto.getPhoneNumber())) {
            throw new BaseException(BaseResponseStatus.USER_ALREADY_EXISTS);
        }

        // 요청에서 받은 부서코드(숫자)를 enum으로 변환
        Department department = Department.fromCode(requestDto.getDepartmentCode());

        // 부서별 고유한 사원번호 생성 (비관적 lock - 동시성 안전)
        String employeeId = generateEmployeeIdWithLock(department);

        // User 엔티티 생성 및 저장
        User user = User.builder()
                .id(employeeId)
                .username(requestDto.getUsername())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .phoneNumber(requestDto.getPhoneNumber())
                .department(department)
                .githubId(requestDto.getGithubId())
                .build();

        userRepository.save(user);
        log.info("새 사용자 등록 완료 - 사원번호: {}, 부서: {}", employeeId, department);
    }

    /**
     * 부서별로 순차적인 사원번호를 안전하게 생성
     * 동시에 여러 요청이 와도 중복되지 않도록 synchronized 처리
     *
     * @param department 부서 enum (CHANNEL, CORE_BANK 등)
     * @return 생성된 사원번호 (예: "CHN0001", "CORE0002")
     */
    private String generateEmployeeIdWithLock(Department department) {
        String prefix = department.getPrefix(); // 부서 코드 (CHN, CORE, EXT, OPR)

        // synchronized: 동시성 문제 방지
        // 같은 시간에 여러 사용자가 가입해도 사원번호가 중복되지 않음
        synchronized(this) {
            // 해당 부서의 마지막 사원번호 조회
            String lastEmployeeId = userRepository.findLastEmployeeIdByPrefix(prefix + "%");

            int nextSequence = 1; // 기본값: 해당 부서 첫 번째 사원

            if (lastEmployeeId != null) {
                // 마지막 사원번호에서 숫자 부분 추출하여 +1
                // 예: "CHN0005" -> "0005" -> 5 -> 6
                String numberPart = lastEmployeeId.substring(prefix.length());
                int lastNumber = Integer.parseInt(numberPart);
                nextSequence = lastNumber + 1;
            }

            // 부서코드 + 4자리 숫자로 사원번호 생성
            // 예: "CHN" + "0001" = "CHN0001"
            String newEmployeeId = prefix + String.format("%04d", nextSequence);

            log.debug("사원번호 생성 - 부서: {}, 마지막번호: {}, 신규번호: {}",
                    department, lastEmployeeId, newEmployeeId);

            return newEmployeeId;
        }
    }
}
