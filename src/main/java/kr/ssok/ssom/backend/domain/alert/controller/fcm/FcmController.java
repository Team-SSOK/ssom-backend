package kr.ssok.ssom.backend.domain.alert.controller.fcm;

import kr.ssok.ssom.backend.domain.alert.dto.fcm.FcmRegisterRequestDto;
import kr.ssok.ssom.backend.domain.alert.service.fcm.FcmService;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * FCM 토큰 등록 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm")
public class FcmController {

    private final FcmService fcmService;

    /**
     * FCM 토큰 등록 API
     *
     * @param userId   사용자 ID (헤더)
     * @param requestDto 디바이스. FCM 토큰 정보
     * @return 등록 결과 응답
     */
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<Void>> registerFcmToken(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody FcmRegisterRequestDto requestDto) {
        fcmService.registerFcmToken(Long.parseLong(userId), requestDto.getToken());
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS));
    }

}
