package kr.ssok.ssom.backend.global.client;

import kr.ssok.ssom.backend.global.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="llm-service", url="${llm.api.url}")
public interface LlmServiceClient {

    // 로그 요약
    @PostMapping("/api/logs/summary")
    LlmApiResponseDto<LogSummaryResponseDto> summarizeLog(@RequestBody LogSummaryRequestDto requestDto);

    // 이슈 초안 작성
    @PostMapping("/api/logs/issues")
    LlmApiResponseDto<LlmIssueResponseDto> writeIssue(@RequestBody LlmIssueRequestDto requestDto);

}
