package kr.ssok.ssom.backend.domain.user.service.Impl;

import kr.ssok.ssom.backend.domain.user.dto.LoginRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.LoginResponseDto;
import kr.ssok.ssom.backend.domain.user.dto.PasswordChangeRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.SignupRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.UserResponseDto;
import kr.ssok.ssom.backend.domain.user.entity.Department;
import kr.ssok.ssom.backend.domain.user.entity.User;
import kr.ssok.ssom.backend.domain.user.security.jwt.JwtTokenProvider;
import kr.ssok.ssom.backend.domain.user.repository.UserRepository;
import kr.ssok.ssom.backend.domain.user.service.UserService;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    // Refresh Token을 저장하는 키 prefix
    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
    // 블랙리스트 토큰을 저장하는 키 prefix
    private static final String BLACKLIST_TOKEN_PREFIX = "blacklist:token:";

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    // 회원가입
    @Override
    @Transactional
    public void registerUser(SignupRequestDto requestDto) {
        // 디버깅용 로그 추가
        log.info("회원가입 요청 - departmentCode: {}", requestDto.getDepartmentCode());
        
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

    // 로그인
    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto requestDto) {
        // 사원번호로 사용자 정보 조회
        User user = findUserByEmployeeId(requestDto.getEmployeeId());

        // 비밀번호 검증
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new BaseException(BaseResponseStatus.INVALID_PASSWORD);
        }

        // 토큰 생성 (사원번호를 userId로 사용)
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // Refresh Token을 Redis에 저장
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + user.getId();
        redisTemplate.opsForValue().set(
                refreshTokenKey,
                refreshToken,
                jwtTokenProvider.getTokenExpirationTime(refreshToken),
                TimeUnit.SECONDS
        );

        log.info("로그인 성공 - 사원번호: {}, 부서: {}", user.getId(), user.getDepartment());

        // 응답 생성
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 토큰 갱신 구현
     * Refresh Token 검증 후 새로운 Access/Refresh Token 발급
     *
     * @param refreshToken 리프레시 토큰
     * @return 새로운 액세스 토큰과 리프레시 토큰이 포함된 응답
     * @throws BaseException 토큰 검증 실패 시 발생
     */
    @Override
    @Transactional
    public LoginResponseDto refreshToken(String refreshToken) {
        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BaseException(BaseResponseStatus.INVALID_REFRESH_TOKEN);
        }

        // 토큰에서 사용자 ID 추출
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // Redis에 저장된 Refresh Token과 비교
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + userId;
        String storedToken = redisTemplate.opsForValue().get(refreshTokenKey);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BaseException(BaseResponseStatus.INVALID_REFRESH_TOKEN);
        }

        // 사용자 정보 조회
        User user = findUserByEmployeeId(userId);

        // 새 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 기존 Refresh Token 삭제
        redisTemplate.delete(refreshTokenKey);

        // 새 Refresh Token을 Redis에 저장
        redisTemplate.opsForValue().set(
                refreshTokenKey,
                newRefreshToken,
                jwtTokenProvider.getTokenExpirationTime(newRefreshToken),
                TimeUnit.SECONDS
        );

        // 응답 생성
        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * 로그아웃 처리 구현
     * Access Token 블랙리스트 추가 및 Refresh Token 삭제
     *
     * @param accessToken 로그아웃할 액세스 토큰
     * @throws BaseException 토큰이 유효하지 않을 경우 발생
     */
    @Override
    public void logout(String accessToken) {
        // 토큰에서 Bearer 제거
        String token = jwtTokenProvider.resolveToken(accessToken);
        if (token == null) {
            throw new BaseException(BaseResponseStatus.INVALID_TOKEN);
        }

        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BaseException(BaseResponseStatus.INVALID_TOKEN);
        }

        // 토큰에서 사용자 ID 추출
        String userId = jwtTokenProvider.getUserIdFromToken(token);

        // Refresh Token 삭제
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(refreshTokenKey);

        // Access Token 블랙리스트에 추가
        long expiration = jwtTokenProvider.getTokenExpirationTime(token);
        String blacklistKey = BLACKLIST_TOKEN_PREFIX + token;

        redisTemplate.opsForValue().set(blacklistKey, "logout", expiration, TimeUnit.SECONDS);

        log.info("로그아웃 성공. 사용자: {}", userId);
    }

    // 회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserInfo(String employeeId) {
        User user = findUserByEmployeeId(employeeId);

        return UserResponseDto.builder()
                .employeeId(user.getId())
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .department(user.getDepartment().name())
                .departmentCode(user.getDepartment().getCode())
                .githubId(user.getGithubId())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public User findUserByEmployeeId(String employeeId) {
        return userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));
    }

    // 비밀번호 변경
    @Override
    @Transactional
    public void changePassword(String employeeId, PasswordChangeRequestDto request) {
        // 1. 새 비밀번호와 확인 비밀번호 일치 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(BaseResponseStatus.PASSWORD_CONFIRM_MISMATCH);
        }
        
        // 2. 사용자 조회
        User user = findUserByEmployeeId(employeeId);
        
        // 3. 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BaseException(BaseResponseStatus.INVALID_CURRENT_PASSWORD);
        }
        
        // 4. 새 비밀번호가 현재 비밀번호와 같은지 확인
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BaseException(BaseResponseStatus.SAME_AS_CURRENT_PASSWORD);
        }
        
        // 5. 새 비밀번호로 변경
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("비밀번호 변경 성공 - 사원번호: {}", employeeId);
    }
}
