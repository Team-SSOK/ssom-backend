package kr.ssok.ssom.backend.domain.user.service;

import jakarta.validation.Valid;
import kr.ssok.ssom.backend.domain.user.dto.LoginRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.LoginResponseDto;
import kr.ssok.ssom.backend.domain.user.dto.SignupRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.UserResponseDto;
import kr.ssok.ssom.backend.domain.user.entity.User;

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
     * 사원번호로 사용자 조회
     */
    User findUserByEmployeeId(String employeeId);
    
    /**
     * 사용자 정보 조회 (DTO 반환)
     */
    UserResponseDto getUserInfo(String employeeId);
}
