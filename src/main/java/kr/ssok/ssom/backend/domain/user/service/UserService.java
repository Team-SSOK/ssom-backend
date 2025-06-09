package kr.ssok.ssom.backend.domain.user.service;

import jakarta.validation.Valid;
import kr.ssok.ssom.backend.domain.user.dto.LoginRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.LoginResponseDto;
import kr.ssok.ssom.backend.domain.user.dto.PasswordChangeRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.SignupRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.UserResponseDto;
import kr.ssok.ssom.backend.domain.user.dto.UserListResponseDto;
import kr.ssok.ssom.backend.domain.user.entity.User;

import java.util.List;

public interface UserService {
    
    /**
     * 사용자 회원가입
     */
    void registerUser(SignupRequestDto requestDto);

    /**
     * 사용자 로그인
     */
    LoginResponseDto login(@Valid LoginRequestDto requestDto);
    
    /**
     * 토큰 갱신
     */
    LoginResponseDto refreshToken(String refreshToken);
    
    /**
     * 로그아웃
     */
    void logout(String accessToken);
    
    /**
     * 사원번호로 사용자 조회
     */
    User findUserByEmployeeId(String employeeId);
    
    /**
     * 사용자 정보 조회 (DTO 반환)
     */
    UserResponseDto getUserInfo(String employeeId);
    
    /**
     * 비밀번호 변경
     */
    void changePassword(String employeeId, PasswordChangeRequestDto request);
    
    /**
     * 모든 사용자 목록 조회
     */
    List<UserListResponseDto> getAllUsers();
}
