package kr.ssok.ssom.backend.domain.logging.service.Impl;

import kr.ssok.ssom.backend.domain.logging.dto.*;
import kr.ssok.ssom.backend.domain.logging.repository.LogSummaryRepository;
import kr.ssok.ssom.backend.domain.logging.service.LoggingService;
import kr.ssok.ssom.backend.global.dto.LogRequestDto;
import kr.ssok.ssom.backend.global.dto.LogSummaryMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingServiceImpl implements LoggingService {

    private final LogSummaryRepository logSummaryRepository;

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

    @Override
    public LogInfoResponseDto getLogInfo(String logId) {
        // DB에 로그 아이디를 검색해 키가 존재할 시 분석 내용 가져오기
        // 없을 시 아무것도 반환하지 않음 (프론트 쪽에서는 분석 버튼 보여주기)
        return null;
    }

    // 로그 상세 조회 중 로그 LLM 요약
    @Override
    public LogSummaryMessageDto summarizeLog(LogRequestDto request) {

        // LLM 쪽 api 이용해서 정보 받아오기 -> dto를 entity로 변환


        // DB에 분석 내용 저장

        // 분석 내용 반환
        return null;
    }
}
