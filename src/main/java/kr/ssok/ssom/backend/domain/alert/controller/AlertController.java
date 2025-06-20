package kr.ssok.ssom.backend.domain.alert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ssok.ssom.backend.domain.alert.dto.*;
import kr.ssok.ssom.backend.domain.alert.service.AlertService;
import kr.ssok.ssom.backend.domain.issue.service.IssueService;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;


@Tag(name = "Alert API", description = "알림 관련 API를 관리하는 컨트롤러")
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;
    private final IssueService issueService;

    @Operation(summary = "알림 SSE 구독", description = "알림에 대해 SSE 구독을 진행합니다.")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
                                @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        log.info("[알림 SSE 구독] 컨트롤러 진입 - User: {}",
                userPrincipal != null ? userPrincipal.getEmployeeId() : "null");

        // 인증되지 않은 사용자 처리
        if (userPrincipal == null) {
            log.error("알림 SSE 구독 실패 - 인증되지 않은 사용자");
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED);
        }

        // SSE 전용 응답 헤더 설정 - 브라우저 호환성 향상
        setupSseHeaders(response);

        // 클라이언트 정보 로깅 (디버깅용)
        logClientInfo(request, userPrincipal.getEmployeeId());

        return alertService.subscribe(userPrincipal.getEmployeeId(), lastEventId, response);
    }

    /**
     * SSE 전용 응답 헤더 설정
     */
    private void setupSseHeaders(HttpServletResponse response) {
        // 캐시 방지
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // 연결 유지
        response.setHeader("Connection", "keep-alive");

        // SSE 관련 헤더
        response.setHeader("Content-Type", "text/event-stream; charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Last-Event-ID");

        // Nginx 등 프록시 버퍼링 방지
        response.setHeader("X-Accel-Buffering", "no");
    }

    /**
     * 클라이언트 정보 로깅 (디버깅용)
     */
    private void logClientInfo(HttpServletRequest request, String employeeId) {
        String userAgent = request.getHeader("User-Agent");
        String clientIp = getClientIpAddress(request);

        log.info("[SSE 클라이언트 정보] employeeId: {}, IP: {}, UserAgent: {}",
                employeeId, clientIp, userAgent);
    }

    /**
     * 클라이언트 실제 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    @Operation(summary = "전체 알림 목록 조회", description = "개별 사용자의 전체 알림 목록을 조회합니다.")
    @GetMapping
    public BaseResponse<List<AlertResponseDto>> getAllAlerts(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("[전체 알림 목록 조회] 컨트롤러 진입");

        // 인증되지 않은 사용자 처리
        if (userPrincipal == null) {
            log.error("전체 알림 목록 조회 실패 - 인증되지 않은 사용자");
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED);
        }

        return new BaseResponse<>(BaseResponseStatus.SUCCESS,
                alertService.getAllAlertsForUser(userPrincipal.getEmployeeId()));
    }

    @Operation(summary = "페이징 알림 목록 조회", description = "개별 사용자의 전체 알림 목록을 페이지네이션하여 조회합니다.")
    @GetMapping("/paged")
    public BaseResponse<Page<AlertResponseDto>> getPagedAlerts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "alert.timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("[페이징 알림 목록 조회] 컨트롤러 진입");

        if (userPrincipal == null) {
            log.error("[페이징 알림 목록 조회] 실패 : 인증되지 않은 사용자");
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED);
        }

        return new BaseResponse<>(BaseResponseStatus.SUCCESS,
                alertService.getPagedAlertsForUser(userPrincipal.getEmployeeId(), pageable));
    }

    @Operation(summary = "알림 개별 상태 변경", description = "알림의 개별 읽음 여부를 변경합니다.")
    @PatchMapping("/modify")
    public BaseResponse<AlertResponseDto> modifyAlertStatus(@RequestBody AlertModifyRequestDto request) {
        log.info("[알림 개별 상태 변경] 컨트롤러 진입");

        return new BaseResponse<>(BaseResponseStatus.SUCCESS,
                alertService.modifyAlertStatus(request));
    }

    @Operation(summary = "알림 일괄 상태 변경", description = "알림의 일괄 읽음 여부를 변경합니다.")
    @PatchMapping("/modifyAll")
    public BaseResponse<List<AlertResponseDto>> modifyAllAlertStatus(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("[알림 일괄 상태 변경] 컨트롤러 진입");

        if (userPrincipal == null) {
            log.error("[알림 일괄 상태 변경] 인증되지 않은 사용자");
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED);
        }

        return new BaseResponse<>(BaseResponseStatus.SUCCESS,
                alertService.modifyAllAlertStatus(userPrincipal.getEmployeeId()));
    }

    @Operation(summary = "알림 개별 삭제", description = "알림을 삭제 처리합니다.")
    @PatchMapping("/delete")
    public BaseResponse<Void> deleteAlert(@RequestBody AlertModifyRequestDto request) {
        log.info("[알림 개별 삭제] 컨트롤러 진입 : ID = {} ", request.getAlertStatusId());

        alertService.deleteAlert(request);
        return new BaseResponse<>(BaseResponseStatus.SUCCESS);
    }

    /*********************************************************************************************************************/
    @Operation(summary = "그라파나 알림", description = "그라파나 알림 데이터를 받아 앱으로 전송합니다.")
    @PostMapping("/grafana")
    public ResponseEntity<BaseResponse<Void>> sendGrafanaAlert(@RequestBody AlertGrafanaRequestDto requestDto) {
        log.info("[그라파나 알림] 컨트롤러 진입");

        // 비동기 처리로 변경 - 즉시 응답
        alertService.createGrafanaAlertAsync(requestDto);
        
        return ResponseEntity
                .status(HttpStatus.ACCEPTED) // 202 Accepted (비동기 처리)
                .body(new BaseResponse<>(BaseResponseStatus.SUCCESS));
    }

    @Operation(summary = "오픈서치 대시보드 알림", description = "오픈서치 대시보드 알림 데이터를 받아 앱으로 전송합니다.")
    @PostMapping("/opensearch")
    public ResponseEntity<BaseResponse<Void>> sendOpensearchAlert(@RequestBody String requestStr) {
        log.info("[오픈서치 대시보드 알림] 컨트롤러 진입");

        // 비동기 처리로 변경 - 즉시 응답
        alertService.createOpensearchAlertAsync(requestStr);
        
        return ResponseEntity
                .status(HttpStatus.ACCEPTED) // 202 Accepted (비동기 처리)
                .body(new BaseResponse<>(BaseResponseStatus.SUCCESS));
    }

    @Operation(summary = "Github 이슈 알림", description = "SSOM 에서 등록한 Github 이슈 opened/reopened/closed 시 앱으로 알림을 전송합니다.")
    @PostMapping("/issue")
    public ResponseEntity<BaseResponse<Void>> sendIssueAlert(@RequestBody AlertIssueRequestDto requestDto) {
        log.info("[Github 이슈 알림] 컨트롤러 진입");

        // 비동기 처리로 변경 - 즉시 응답
        alertService.createIssueAlertAsync(requestDto);
        issueService.updateGitHubIssueStatus(requestDto); // 이슈 상태 업데이트는 동기 처리

        return ResponseEntity
                .status(HttpStatus.ACCEPTED) // 202 Accepted (비동기 처리)
                .body(new BaseResponse<>(BaseResponseStatus.SUCCESS));
    }

    @Operation(summary = "Devops 알림", description = "Devops (Jenkins 및 argoCD) 작업 완료 시 앱으로 알림을 전송합니다.")
    @PostMapping("/devops")
    public ResponseEntity<BaseResponse<Void>> sendDevopsAlert(@RequestBody AlertDevopsRequestDto requestDto) {
        log.info("[Devops 알림] 컨트롤러 진입");

        // 비동기 처리로 변경 - 즉시 응답
        alertService.createDevopsAlertAsync(requestDto);
        
        return ResponseEntity
                .status(HttpStatus.ACCEPTED) // 202 Accepted (비동기 처리)
                .body(new BaseResponse<>(BaseResponseStatus.SUCCESS));
    }

    // 비동기 방식 오픈서치 대시보드 알림
    @Operation(summary = "오픈서치 대시보드 알림", description = "오픈서치 대시보드 알림 데이터를 받아 앱으로 전송합니다.")
    @PostMapping("/opensearch/sync")
    public ResponseEntity<BaseResponse<Void>> sendOpensearchAlertSync(@RequestBody String requestStr) {
        log.info("[오픈서치 대시보드 알림] 컨트롤러 진입");

        alertService.createOpensearchAlert(requestStr);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new BaseResponse<>(BaseResponseStatus.SUCCESS));
    }
}
