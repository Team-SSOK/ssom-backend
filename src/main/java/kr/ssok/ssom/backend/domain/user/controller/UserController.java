package kr.ssok.ssom.backend.domain.user.controller;

import jakarta.validation.Valid;
import kr.ssok.ssom.backend.domain.user.dto.LoginRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.SignupRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.LoginResponseDto;
import kr.ssok.ssom.backend.domain.user.dto.UserResponseDto;
import kr.ssok.ssom.backend.domain.user.service.UserService;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<Void>> registerUser(
            @RequestBody SignupRequestDto requestDto) {
        userService.registerUser(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(BaseResponseStatus.REGISTER_USER_SUCCESS));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto requestDto) {
        log.info("로그인 요청. 사원번호 ID: {}", requestDto.getEmployeeId());
        LoginResponseDto responseDto = userService.login(requestDto);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.LOGIN_SUCCESS, responseDto));
    }

    // 로그인 (지문인식)

    // 비밀번호 변경

    // 회원정보 조회
    @GetMapping("/{employeeId}")
    public ResponseEntity<BaseResponse<UserResponseDto>> getUserProfile(
            @PathVariable String employeeId) {
        log.info("사용자 정보 조회 요청. 사원번호: {}", employeeId);
        
        UserResponseDto userInfo = userService.getUserInfo(employeeId);
        
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, userInfo));
    }
}
