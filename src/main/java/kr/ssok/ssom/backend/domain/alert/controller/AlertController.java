package kr.ssok.ssom.backend.domain.alert.controller;

import io.swagger.v3.oas.annotations.Operation;
import kr.ssok.ssom.backend.domain.alert.dto.AlertModifyRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertRequestDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertResponseDto;
import kr.ssok.ssom.backend.domain.alert.dto.AlertSendRequestDto;
import kr.ssok.ssom.backend.domain.alert.entity.constant.AlertKind;
import kr.ssok.ssom.backend.domain.alert.service.AlertService;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "전체 알림 목록 조회", description = "전체 알림 목록을 조회합니다.")
    @GetMapping
    public BaseResponse<List<AlertResponseDto>> getAllAlerts(@RequestParam Long userId) {
        log.info("[전체 알림 목록 조회] 컨트롤러 진입");

        return new BaseResponse<>(BaseResponseStatus.SUCCESS, alertService.getAllAlertsForUser(userId));
    }

    @Operation(summary = "알림 상태 변경", description = "알림의 읽음 여부를 변경합니다.")
    @PatchMapping("/modify")
    public BaseResponse<Void> modifyAlertStatus(@RequestBody AlertModifyRequestDto request) {
        log.info("[알림 상태 변경] 컨트롤러 진입");

        alertService.modifyAlertStatus(request);
        return new BaseResponse<>(BaseResponseStatus.SUCCESS);
    }

    @Operation(summary = "그라파나 알림", description = "그라파나 알림 데이터를 받아 앱으로 전송합니다.")
    @PostMapping("/grafana")
    public BaseResponse<List<AlertResponseDto>> getGrafanaAlerts(@RequestBody AlertRequestDto alertRequest) {
        log.info("[그라파나 알림] 컨트롤러 진입");

        List<AlertResponseDto> responses = alertService.createAlert(alertRequest, AlertKind.GRAFANA);
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, responses);
    }

    @Operation(summary = "오픈서치 대시보드 알림", description = "오픈서치 대시보드 알림 데이터를 받아 앱으로 전송합니다.")
    @PostMapping("/opensearch")
    public BaseResponse<List<AlertResponseDto>> sendOpenSearchAlert(@RequestBody AlertRequestDto alertRequest) {
        log.info("[오픈서치 대시보드 알림] 컨트롤러 진입");

        List<AlertResponseDto> responses = alertService.createAlert(alertRequest, AlertKind.OPENSEARCH);
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, responses);
    }

    @Operation(summary = "이슈 생성 알림", description = "이슈 생성 시 앱으로 알림을 전송합니다.")
    @PostMapping("/issue")
    public BaseResponse<List<AlertResponseDto>> sendIssueAlert(@RequestBody AlertRequestDto alertRequest) {
        log.info("[이슈 생성 알림] 컨트롤러 진입");

        List<AlertResponseDto> responses = alertService.createAlert(alertRequest, AlertKind.ISSUE);
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, responses);
    }

    @Operation(summary = "Jenkins 및 argoCD 알림", description = "Jenkins 및 argoCD 작업 완료 시 앱으로 알림을 전송합니다.")
    @PostMapping("/send")
    public BaseResponse<List<AlertResponseDto>> sendAlert(@RequestBody AlertRequestDto alertRequest) {
        //TODO : Jenkins 와 argoCD api 분리 여부?

        log.info("[Jenkins 및 argoCD 알림] 컨트롤러 진입");

        List<AlertResponseDto> responses = alertService.createAlert(alertRequest, AlertKind.JENKINS);
       // List<AlertResponseDto> responses = alertService.createAlert(alertRequest, AlertKind.ARGOCD);
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, responses);
    }
}
