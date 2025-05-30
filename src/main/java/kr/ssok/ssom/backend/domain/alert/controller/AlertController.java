package kr.ssok.ssom.backend.domain.alert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.alert.dto.*;
import kr.ssok.ssom.backend.domain.alert.service.AlertService;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
@Slf4j
public class AlertController {
 // TODO class 에 swagger 적용

    private final AlertService alertService;

    @Operation(summary = "알림 SSE 구독", description = "알림에 대해 SSE 구독을 진행합니다.")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
                                @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId,
                                HttpServletResponse response) {
        log.info("[알림 SSE 구독] 컨트롤러 진입");

        return alertService.subscribe(userPrincipal.getEmployeeId(), lastEventId, response);
    }

    @Operation(summary = "전체 알림 목록 조회", description = "전체 알림 목록을 조회합니다.")
    @GetMapping
    public BaseResponse<List<AlertResponseDto>> getAllAlerts(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("[전체 알림 목록 조회] 컨트롤러 진입");

        return new BaseResponse<>(BaseResponseStatus.SUCCESS,
                alertService.getAllAlertsForUser(userPrincipal.getEmployeeId()));
    }

    @Operation(summary = "알림 상태 변경", description = "알림의 읽음 여부를 변경합니다.")
    @PatchMapping("/modify")
    public BaseResponse<Void> modifyAlertStatus(@RequestBody AlertModifyRequestDto request) {
        log.info("[알림 상태 변경] 컨트롤러 진입");

        alertService.modifyAlertStatus(request);
        return new BaseResponse<>(BaseResponseStatus.SUCCESS);
    }

    /*********************************************************************************************************************/
    @Operation(summary = "그라파나 알림", description = "그라파나 알림 데이터를 받아 앱으로 전송합니다.")
    @PostMapping("/grafana")
    public BaseResponse<List<AlertResponseDto>> sendGrafanaAlert(@RequestBody AlertGrafanaRequestDto alertGrafanaRequestDto) {
        log.info("[그라파나 알림] 컨트롤러 진입");

        List<AlertResponseDto> responses = alertService.createGrafanaAlert(alertGrafanaRequestDto);
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, responses);
    }

    @Operation(summary = "오픈서치 대시보드 알림", description = "오픈서치 대시보드 알림 데이터를 받아 앱으로 전송합니다.")
    @PostMapping("/opensearch")
    public ResponseEntity<BaseResponse<Void>> sendOpensearchAlert(@RequestBody AlertOpensearchRequestDto requestDto) {
        log.info("[오픈서치 대시보드 알림] 컨트롤러 진입");

        alertService.createOpensearchAlert(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new BaseResponse<>(BaseResponseStatus.SUCCESS));
    }

    @Operation(summary = "이슈 생성 알림", description = "이슈 생성 시 앱으로 알림을 전송합니다.")
    @PostMapping("/issue")
    public BaseResponse<List<AlertResponseDto>> sendIssueAlert(@RequestBody AlertIssueRequestDto AlertIssueRequest) {
        log.info("[이슈 생성 알림] 컨트롤러 진입");

        List<AlertResponseDto> responses = alertService.createIssueAlert(AlertIssueRequest);
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, responses);
    }

    /*
    @Operation(summary = "Jenkins 및 argoCD 알림", description = "Jenkins 및 argoCD 작업 완료 시 앱으로 알림을 전송합니다.")
    @PostMapping("/send")
    public BaseResponse<List<AlertResponseDto>> sendDevopsAlert(@RequestBody AlertSendRequestDto alertSendRequest) {
        log.info("[Jenkins 및 argoCD 알림] 컨트롤러 진입");

        List<AlertResponseDto> responses = alertService.createDevopsAlert(alertSendRequest);
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, responses);
    }*/
}
