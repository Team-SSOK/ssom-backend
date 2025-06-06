package kr.ssok.ssom.backend.domain.issue.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ssok.ssom.backend.domain.issue.dto.GitHubIssueCreateRequestDto;
import kr.ssok.ssom.backend.domain.issue.dto.IssueCreateRequestDto;
import kr.ssok.ssom.backend.domain.issue.dto.IssueResponseDto;
import kr.ssok.ssom.backend.domain.issue.service.IssueService;
import kr.ssok.ssom.backend.domain.user.security.principal.UserPrincipal;
import kr.ssok.ssom.backend.global.dto.LlmIssueResponseDto;
import kr.ssok.ssom.backend.global.exception.BaseException;
import kr.ssok.ssom.backend.global.exception.BaseResponse;
import kr.ssok.ssom.backend.global.exception.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Issue 관련 API Controller
 * 
 * SSOM 앱에서 호출하는 Issue 관련 모든 API 엔드포인트를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Tag(name = "Issue", description = "Issue 관리 API - LLM 기반 자동 이슈 생성 및 GitHub 연동")
public class IssueController {
    
    private final IssueService issueService;
    
    /**
     * LLM을 통한 Issue 초안 작성
     */
    @Operation(summary = "LLM Issue 초안 작성", description = "선택한 로그들을 LLM으로 분석하여 GitHub Issue 초안을 자동 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Issue 초안 작성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (로그 ID 없음 등)"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "500", description = "LLM API 호출 실패")
    })
    @PostMapping("/draft")
    public ResponseEntity<BaseResponse<LlmIssueResponseDto>> createIssueDraft(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody IssueCreateRequestDto request) {
        
        log.info("Issue 초안 작성 API 호출 - 사원번호: {}, 로그 개수: {}", 
                userPrincipal.getEmployeeId(), request.getLogIds().size());
        
        try {
            LlmIssueResponseDto response = issueService.createIssueDraft(request, userPrincipal.getEmployeeId());
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));
        } catch (Exception e) {
            log.error("Issue 초안 작성 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseResponse<>(BaseResponseStatus.LLM_API_ERROR));
        }
    }
    
    /**
     * GitHub Issue 생성
     */
    @Operation(summary = "GitHub Issue 생성", description = "LLM이 작성한 초안을 바탕으로 실제 GitHub Repository에 Issue를 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "GitHub Issue 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락 등)"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "500", description = "GitHub API 호출 실패")
    })
    @PostMapping("/github")
    public ResponseEntity<BaseResponse<IssueResponseDto>> createGitHubIssue(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody GitHubIssueCreateRequestDto request) {
        
        log.info("GitHub Issue 생성 API 호출 - 사원번호: {}, 제목: {}", 
                userPrincipal.getEmployeeId(), request.getTitle());
        
        try {
            IssueResponseDto response = issueService.createGitHubIssue(request, userPrincipal.getEmployeeId());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(BaseResponseStatus.CREATED, response));
        } catch (Exception e) {
            log.error("GitHub Issue 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseResponse<>(BaseResponseStatus.GITHUB_API_ERROR));
        }
    }
    
    /**
     * 내가 담당자로 지정된 Issue 목록 조회
     */
    @Operation(summary = "내가 담당자로 지정된 Issue 목록 조회", description = "현재 로그인한 사용자가 담당자로 지정된 Issue 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Issue 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/my")
    public ResponseEntity<BaseResponse<List<IssueResponseDto>>> getMyIssues(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("내 담당 Issue 목록 조회 API 호출 - 사원번호: {}", userPrincipal.getEmployeeId());
        
        try {
            List<IssueResponseDto> response = issueService.getMyIssues(userPrincipal.getEmployeeId());
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));
        } catch (Exception e) {
            log.error("내 담당 Issue 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }
    
    /**
     * 전체 Issue 목록 조회
     */
    @Operation(summary = "전체 Issue 목록 조회", description = "팀에서 공유하는 전체 Issue 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "전체 Issue 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<List<IssueResponseDto>>> getAllIssues(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("전체 Issue 목록 조회 API 호출 - 요청자: {}", userPrincipal.getEmployeeId());
        
        try {
            List<IssueResponseDto> response = issueService.getAllIssues(userPrincipal.getEmployeeId());
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));
        } catch (Exception e) {
            log.error("전체 Issue 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }
    
    /**
     * 특정 Issue 상세 정보 조회
     */
    @Operation(summary = "Issue 상세 정보 조회", description = "특정 Issue의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Issue 상세 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "404", description = "Issue를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{issueId}")
    public ResponseEntity<BaseResponse<IssueResponseDto>> getIssue(
            @Parameter(description = "Issue ID") @PathVariable Long issueId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("Issue 상세 조회 API 호출 - Issue ID: {}, 요청자: {}", 
                issueId, userPrincipal.getEmployeeId());
        
        try {
            IssueResponseDto response = issueService.getIssue(issueId, userPrincipal.getEmployeeId());
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));
        } catch (Exception e) {
            log.error("Issue 상세 조회 실패: {}", e.getMessage());
            if (e.getMessage().contains("NOT_FOUND")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BaseResponse<>(BaseResponseStatus.NOT_FOUND_ISSUE));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }
}
