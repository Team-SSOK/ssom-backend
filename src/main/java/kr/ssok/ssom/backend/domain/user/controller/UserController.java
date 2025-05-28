package kr.ssok.ssom.backend.domain.user.controller;

import kr.ssok.ssom.backend.domain.user.dto.SignupRequestDto;
import kr.ssok.ssom.backend.domain.user.dto.SignupResponseDto;
import kr.ssok.ssom.backend.domain.user.service.UserService;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
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


    // 로그인 (지문인식)

    // 비밀번호 변경

    // 회원정보 조회
}
