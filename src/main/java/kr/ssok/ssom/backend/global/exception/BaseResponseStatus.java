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
    INVALID_REQUEST(false, 4002, "유효하지 않은 요청입니다"),

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

    USER_NOT_FOUND(false, 5011, "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(false, 5010, "이미 존재하는 사용자입니다.");

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
