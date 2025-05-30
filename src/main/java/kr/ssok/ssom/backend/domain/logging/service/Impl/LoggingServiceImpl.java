package kr.ssok.ssom.backend.domain.logging.service.Impl;

import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.domain.logging.entity.LogSummary;
import kr.ssok.ssom.backend.domain.logging.repository.LogSummaryRepository;
import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.global.client.LlmServiceClient;
import kr.ssok.ssom.backend.global.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingServiceImpl implements LoggingService {

    private final LogSummaryRepository logSummaryRepository;
    private final LlmServiceClient llmServiceClient;

    @Override
    public ServicesResponseDto getServices() {
        // OpenSearch에 요청 보내기?
        return null;
    }

    @Override
    public LogsResponseDto getLogs() {
        // 초기: 모든 서비스, 모든 레벨

        // 필터링 시: 조건에 맞는 서비스와 레벨만 조회
        return null;
    }

    /**
     * 로그 상세 조회 - 이전에 생성한 LLM 요약 반환
     */
    @Override
    public LogSummaryMessageDto getLogInfo(String logId) {

        // DB에 로그 아이디로 조회
        Optional<LogSummary> summaryOpt = logSummaryRepository.findByLogId(logId);

        // 없을 시 아무것도 반환하지 않음
        if (summaryOpt.isEmpty()) {
            return null;
        }

        LogSummary summaryEntity = summaryOpt.get();

        LogLocationDto locationdto = LogLocationDto.builder()
                .file(summaryEntity.getFileLocation())
                .function(summaryEntity.getFunctionLocation())
                .build();
        LogSummaryMessageDto summaryDto = LogSummaryMessageDto.builder()
                .title(summaryEntity.getTitle())
                .location(locationdto)
                .solution(summaryEntity.getSolution())
                .build();

        return summaryDto;
    }

    /**
     * 로그 상세 조회 - 새롭게 생성한 LLM 요약 반환
     */
    @Override
    public LogSummaryMessageDto summarizeLog(LogDto request) {

        // LLM 쪽으로 api 요청
        LogRequestDto requestDto = LogRequestDto.builder()
                .level(request.getLevel())
                .logger(request.getLogger())
                .thread(request.getThread())
                .message(request.getMessage())
                .app(request.getApp())
                .build();
        LlmApiRequestDto llmRequestDto = LlmApiRequestDto.builder()
                .logs(List.of(requestDto))
                .build();
        LlmApiResponseDto<LogSummaryResponseDto> llmResponseDto = llmServiceClient.summarizeLog(llmRequestDto);

        // 응답에서 요약 메시지 추출
        LogSummaryMessageDto summaryDto = llmResponseDto.getResult().get(0).getMessage();

        // DB에 저장
        LogSummary summaryEntity = LogSummary.builder()
                .logId(request.getLogId())
                .title(summaryDto.getTitle())
                .fileLocation(summaryDto.getLocation().getFile())
                .functionLocation(summaryDto.getLocation().getFunction())
                .solution(summaryDto.getSolution())
                .build();
        logSummaryRepository.save(summaryEntity);

        // 분석 내용 반환
        return summaryDto;
    }
}
