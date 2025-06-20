package kr.ssok.ssom.backend.domain.alert.service;

import kr.ssok.ssom.backend.domain.alert.dto.*;
import java.util.List;
import java.util.Map;

/**
 * Alert 성능 테스트용 서비스 인터페이스
 * JMeter를 사용한 동기/비동기 처리 성능 비교를 위한 메서드 정의
 */
public interface AlertPerformanceTestService {
    
    // ======================================
    // 동기 처리 테스트 메서드들
    // ======================================
    
    /**
     * 동기 방식으로 그라파나 알림 처리
     * @param requestDto 그라파나 알림 요청 데이터
     * @param targetUserCount 대상 사용자 수 (0이면 전체 사용자)
     * @return 처리 결과 정보
     */
    Map<String, Object> processSyncGrafanaAlert(AlertGrafanaRequestDto requestDto, int targetUserCount);

    /**
     * 동기 방식으로 오픈서치 알림 처리
     * @param requestStr 오픈서치 알림 요청 문자열
     * @param targetUserCount 대상 사용자 수 (0이면 전체 사용자)
     * @return 처리 결과 정보
     */
    Map<String, Object> processSyncOpensearchAlert(String requestStr, int targetUserCount);

    /**
     * 동기 방식으로 DevOps 알림 처리
     * @param requestDto DevOps 알림 요청 데이터
     * @param targetUserCount 대상 사용자 수 (0이면 전체 사용자)
     * @return 처리 결과 정보
     */
    Map<String, Object> processSyncDevopsAlert(AlertDevopsRequestDto requestDto, int targetUserCount);

    // ======================================
    // 비동기 처리 테스트 메서드들
    // ======================================
    
    /**
     * 비동기 방식으로 그라파나 알림 처리
     * @param requestDto 그라파나 알림 요청 데이터
     * @param targetUserCount 대상 사용자 수 (0이면 전체 사용자)
     * @return 처리 결과 정보 (즉시 반환)
     */
    Map<String, Object> processAsyncGrafanaAlert(AlertGrafanaRequestDto requestDto, int targetUserCount);

    /**
     * 비동기 방식으로 오픈서치 알림 처리
     * @param requestStr 오픈서치 알림 요청 문자열
     * @param targetUserCount 대상 사용자 수 (0이면 전체 사용자)
     * @return 처리 결과 정보 (즉시 반환)
     */
    Map<String, Object> processAsyncOpensearchAlert(String requestStr, int targetUserCount);

    /**
     * 비동기 방식으로 DevOps 알림 처리
     * @param requestDto DevOps 알림 요청 데이터
     * @param targetUserCount 대상 사용자 수 (0이면 전체 사용자)
     * @return 처리 결과 정보 (즉시 반환)
     */
    Map<String, Object> processAsyncDevopsAlert(AlertDevopsRequestDto requestDto, int targetUserCount);

    // ======================================
    // 테스트 지원 메서드들
    // ======================================
    
    /**
     * 테스트용 샘플 데이터 생성
     * @param alertType 알림 타입 (grafana, opensearch, devops)
     * @param count 생성할 개수
     * @return 생성된 테스트 데이터 리스트
     */
    List<Object> generateTestData(String alertType, int count);

    /**
     * 현재 시스템 상태 조회
     * @return 시스템 상태 정보 (SSE 연결 수, 큐 상태 등)
     */
    Map<String, Object> getSystemStatus();

    /**
     * 성능 메트릭 조회
     * @param durationMinutes 조회할 시간 범위 (분)
     * @return 성능 메트릭 정보
     */
    Map<String, Object> getPerformanceMetrics(int durationMinutes);

    /**
     * 테스트 환경 초기화
     * @return 초기화 결과 정보
     */
    Map<String, Object> resetTestEnvironment();
}
