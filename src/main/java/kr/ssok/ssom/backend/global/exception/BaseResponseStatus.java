package kr.ssok.ssom.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BaseResponseStatus {

    SUCCESS(true,2000, "요청에 성공하였습니다."),
    CREATED(true, 2001, "생성되었습니다."),
    REGISTER_USER_SUCCESS(true, 2002, "회원가입에 성공하였습니다."),
    LOGIN_SUCCESS(true, 2003, "로그인에 성공하였습니다."),
    LOGOUT_SUCCESS(true, 2004, "로그아웃에 성공하였습니다."),
    TOKEN_REFRESH_SUCCESS(true, 2005, "토큰 갱신에 성공하였습니다."),
    PASSWORD_CHANGE_SUCCESS(true, 2006, "비밀번호 변경에 성공하였습니다."),

    BAD_REQUEST(false, 4000, "잘못된 요청입니다."),
    INVALID_PARAMETER(false, 4001, "유효하지 않은 파라미터입니다."),
    INVALID_REQUEST(false, 4002, "유효하지 않은 요청입니다."),
    UNAUTHORIZED(false, 4003, "인증되지 않은 사용자입니다."),

    // 회원 관련 오류
    INVALID_SIGNUP_REQUEST_VALUE(false, 4002, "유효하지 않은 회원가입 양식입니다."),
    INVALID_PASSWORD(false, 4000, "유효하지 않은 비밀번호입니다."),
    INVALID_CURRENT_PASSWORD(false, 4003, "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_CONFIRM_MISMATCH(false, 4004, "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    SAME_AS_CURRENT_PASSWORD(false, 4005, "새 비밀번호가 현재 비밀번호와 동일합니다."),
    CODE_VERIFICATION_FAIL(false, 4001, "휴대폰 인증번호가 일치하지 않아, 인증에 실패했습니다."),
    BIOMETRIC_ALREADY_REGISTERED(false, 4006, "이미 등록된 생체인증이 있습니다."),
    BIOMETRIC_NOT_REGISTERED(false, 4007, "생체인증이 등록되어있지 않습니다."),
    INVALID_BIOMETRIC(false, 4008, "유효하지 않은 생체인증입니다."),
    BIOMETRIC_NOT_FOUND(false, 4009, "생체인증 정보를 찾을 수 없습니다."),

    // 인증 관련 오류
    INVALID_TOKEN(false, 4010, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(false, 4011, "만료된 토큰입니다."),
    INVALID_REFRESH_TOKEN(false, 4012, "유효하지 않은 리프레시 토큰입니다."),
    BLACKLISTED_TOKEN(false, 4013, "이미 로그아웃된 토큰입니다."),
    LOGIN_FAILED(false, 4014, "로그인에 실패했습니다."),
    TOO_MANY_LOGIN_ATTEMPTS(false, 4015, "로그인 시도 횟수가 초과되었습니다. 잠시 후 다시 시도해주세요."),
    ACCOUNT_LOCKED(false, 4016, "계정이 잠금 상태입니다."),
    PIN_CHANGE_AUTH_REQUIRED(false, 4017, "PIN 번호 변경을 위한 인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    PHONE_NUMBER_MISMATCH(false, 4018, "요청된 전화번호가 사용자 정보와 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    BIOMETRIC_MAX_ATTEMPTS_EXCEEDED(false, 4019, "생체인증 시도횟수가 초과되었습니다.", HttpStatus.LOCKED),
    BIOMETRIC_DEVICE_BLOCKED(false, 4020, "디바이스가 차단되었습니다.", HttpStatus.LOCKED),
    FORBIDDEN(false, 4021, "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    USER_NOT_FOUND(false, 5011, "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(false, 5010, "이미 존재하는 사용자입니다."),
    NOT_FOUND_USER(false, 5012, "해당 사용자를 찾을 수 없습니다."),

    // Issue 관련 오류
    NOT_FOUND_ISSUE(false, 6001, "Issue를 찾을 수 없습니다."),
    LLM_API_ERROR(false, 6003, "LLM API 호출 중 오류가 발생했습니다."),
    NOT_FOUND_LOG(false, 6004, "로그를 찾을 수 없습니다."),
    GITHUB_API_ERROR(false, 6005, "GitHub API 호출 중 오류가 발생했습니다."),
    INVALID_ISSUE_ACTION(false, 6006, "지원하지 않는 action 값 입니다."),

    // 알림 관련 오류
    SSE_BAD_REQUEST(false, 7001, "SSE 구독을 위한 사용자 정보가 전달되지 않았습니다."),
    SSE_INIT_ERROR(false, 7002, "SSE 구독 처리 중 오류가 발생했습니다."),
    NOT_FOUND_ALERT(false, 7003, "Alert를 찾을 수 없습니다."),
    PARSING_ERROR(false, 7004, "Json Parsing 오류가 발생했습니다."),
    ALERT_TARGET_USER_NOT_FOUND(false, 7005, "Github 이슈 알림 공유 대상자가 조회되지 않습니다."),
    ALERT_CREATE_FAILED(false, 7006, "알림 생성 중 오류가 발생했습니다."),
    UNSUPPORTED_ALERT_KIND(false, 7007, "알림 유형이 유효하지 않습니다."),
    REDIS_ACCESS_FAILED(false, 7008, "Redis 접근에 실패하였습니다."),

    // Logging 관련 오류
    SERVICES_READ_FAILED(false, 8001, "OpenSearch에서 서비스 목록 조회에 실패했습니다."),
    LOGS_READ_FAILED(false, 8002, "OpenSearch에서 로그 목록 조회에 실패했습니다."),
    LOG_SUMMARY_NOT_FOUND(false, 8003, "기존에 생성된 LLM 요약이 없습니다."),
    LLM_SUMMARY_FAILED(false, 8004, "LLM 서비스를 이용한 로그 분석 생성에 실패했습니다."),
    LLM_SUMMARY_SAVE_FAILED(false, 8005, "LLM 로그 분석을 저장하는 데 실패했습니다."),
    LOG_NOT_FOUND(false, 8006, "로그 ID로 로그를 조회하는 데 실패했습니다."),

    // 서버 오류
    INTERNAL_SERVER_ERROR(false, 5000, "서버 내부 오류가 발생했습니다.");

    private final boolean isSuccess;
    private final int code;
    private final String message;
    private HttpStatus httpStatus;

    BaseResponseStatus(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }

    BaseResponseStatus(boolean isSuccess, int code, String message, HttpStatus httpStatus) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
