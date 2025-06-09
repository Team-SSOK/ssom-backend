package kr.ssok.ssom.backend.domain.alert.controller.fcm;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ssok.ssom.backend.domain.alert.dto.fcm.FcmRegisterRequestDto;
import kr.ssok.ssom.backend.domain.alert.service.fcm.FcmService;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * FCM 토큰 등록 컨트롤러
 */

@Tag(name = "FCM API", description = "FCM 관련 API를 관리하는 컨트롤러")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm")
public class FcmController {

    private final FcmService fcmService;

    /**
     * FCM 토큰 등록 API
     *
     * @param userPrincipal
     * @param requestDto 디바이스. FCM 토큰 정보
     * @return 등록 결과 응답
     */
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<Void>> registerFcmToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody FcmRegisterRequestDto requestDto) {

        log.info("[FCM 토큰 등록 API] 컨트롤러 진입 - User: {}",
                userPrincipal != null ? userPrincipal.getEmployeeId() : "null");

        // 인증되지 않은 사용자 처리
        if (userPrincipal == null) {
            log.error("알림 SSE 구독 실패 - 인증되지 않은 사용자");
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED);
        }

        fcmService.registerFcmToken(userPrincipal.getEmployeeId(), requestDto.getToken());
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS));
    }

}
