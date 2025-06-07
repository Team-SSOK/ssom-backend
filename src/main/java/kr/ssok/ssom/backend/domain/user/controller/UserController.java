package kr.ssok.ssom.backend.domain.user.controller;

import jakarta.validation.Valid;
import kr.ssok.ssom.backend.domain.user.dto.*;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import kr.ssok.ssom.backend.domain.user.service.UserService;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<LoginResponseDto>> refreshToken(@Valid @RequestBody TokenRefreshRequestDto requestDto) {
        log.info("토큰 갱신 요청");
        LoginResponseDto responseDto = userService.refreshToken(requestDto.getRefreshToken());

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.TOKEN_REFRESH_SUCCESS, responseDto));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(@RequestHeader("Authorization") String authorization) {
        log.info("로그아웃 요청");
        userService.logout(authorization);
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.LOGOUT_SUCCESS));
    }

    // 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<BaseResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PasswordChangeRequestDto requestDto) {
        
        log.info("비밀번호 변경 요청. 사원번호: {}", userPrincipal.getEmployeeId());
        
        // @AuthenticationPrincipal로 토큰에서 추출한 사용자 정보 사용
        userService.changePassword(userPrincipal.getEmployeeId(), requestDto);
        
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.PASSWORD_CHANGE_SUCCESS));
    }

    // 회원정보 조회 (본인 정보)
    @GetMapping("/profile")
    public ResponseEntity<BaseResponse<UserResponseDto>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("사용자 정보 조회 요청. 사원번호: {}", userPrincipal.getEmployeeId());
        
        UserResponseDto userInfo = userService.getUserInfo(userPrincipal.getEmployeeId());
        
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, userInfo));
    }

    // 모든 사용자 목록 조회
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<UserListResponseDto>>> getAllUsers() {

        log.info("모든 사용자 목록 조회 요청");

        List<UserListResponseDto> userList = userService.getAllUsers();

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, userList));
    }

    // 부서별 유저 정보
}
