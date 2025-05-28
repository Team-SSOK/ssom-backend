package kr.ssok.ssom.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BaseResponseStatus {

    /**
     * 1. 요청에 성공한 경우(2000)
     */
    // 0. 공통
    SUCCESS(true, 2000, "요청에 성공하였습니다."),

    // 1-1. 회원가입 / 로그인 / 로그아웃 / 회원탈퇴
    REGISTER_ACCOUNT_SUCCESS(true, 2100, "회원가입이 완료되었습니다"),
    LOGIN_SUCCESS(true, 2101, "로그인이 완료되었습니다."),
    KAKAO_LOGIN_SUCCESS(true, 2102, "카카오 로그인이 완료되었습니다."),
    GOOGLE_LOGIN_SUCCESS(true, 2103, "구글 로그인이 완료되었습니다."),
    LOGOUT_SUCCESS(true, 2104, "로그아웃이 완료되었습니다."),
    DELETE_ACCOUNT_SUCCESS(true, 2105, "회원탈퇴가 완료되었습니다."),

    // 1-2. 펀딩
    FUNDING_ITEM_LINK_SUCCESS(true, 2200, "펀딩 아이템이 저장되었습니다."),
    FUNDING_CREATE_SUCCESS(true, 2201, "펀딩 등록이 완료되었습니다."),
    FUNDING_FINISH_SUCCESS(true, 2202, "펀딩 종료가 완료되었습니다."),
    FUNDING_UPDATE_SUCCESS(true, 2203, "펀딩 수정이 완료되었습니다."),
    FUNDING_DELETE_SUCCESS(true, 2204, "펀딩 삭제가 완료되었습니다."),
    MY_FUNDING_GET_SUCCESS(true, 2205, "내 펀딩 조회가 완료되었습니다."),
    ACTIVE_MAIN_FUNDING_GET_SUCCESS(true, 2206, "메인 페이지 진행중인 펀딩 조회가 완료되었습니다."),
    ALL_FUNDING_GET_SUCCESS(true, 2207, "전체 펀딩 조회가 완료되었습니다."),
    ACTIVE_FUNDINGS_GET_SUCCESS(true, 2208, "진행중인 모든 펀딩 조회가 완료되었습니다."),
    FINISHED_FUNDINGS_GET_SUCCESS(true, 2209, "종료된 모든 펀딩 조회가 완료되었습니다."),
    FUNDING_DETAIL_GET_SUCCESS(true, 2210, "펀딩 상세페이지 조회가 완료되었습니다."),
    FUNDINGS_SUMMARY_GET_SUCCESS(true, 2211, "펀딩 통계 정보 조회가 완료되었습니다."),

    // 1-3. 후원
    DONATION_RANKING_DELIVERY_SUCCESS(true, 2300, "후원 랭킹 전달이 완료되었습니다."),
    DONATION_READY_SUCCESS(true, 2301, "후원 결제 준비 요청이 완료되었습니다."),
    DONATION_APPROVE_SUCCESS(true, 2302, "후원 결제 승인 요청이 완료되었습니다."),
    DONATION_CANCEL(true, 2303, "후원 결제가 취소되었습니다."), // 클라이언트 요청으로 200 & true 설정
    DONATION_FAIL(true, 2304, "후원 결제에 실패했습니다."), // 클라이언트 요청으로 200 & true 설정

    /**
     * 2. 클라이언트 에러(4000)
     */
    // 0. 공통
    BAD_REQUEST(false, 4000, "잘못된 요청입니다."),

    // 2-1. 회원가입 / 로그인 / 로그아웃 / 회원탈퇴
    EMAIL_ALREADY_EXISTS(false, 4100, "이미 가입된 이메일입니다."),
    LOGIN_FAILED(false, 4101, "로그인에 실패했습니다. 다시 로그인을 진행해 주세요."),
    KAKAO_LOGIN_FAILED(false, 4102, "카카오 로그인에 실패했습니다. 다시 로그인을 진행해 주세요."),
    GOOGLE_LOGIN_FAILED(false, 4103, "구글 로그인에 실패했습니다. 다시 로그인을 진행해 주세요."),
    USER_NOT_FOUND(false, 4104, "가입된 유저 정보가 없습니다."),
    PASSWORD_MISMATCH(false, 4105, "비밀번호가 일치하지 않습니다."),
    LOGOUT_FAILED(false, 4106, "로그아웃에 실패했습니다."),
    DELETE_ACCOUNT_FAILED(false, 4107, "회원탈퇴에 실패했습니다."),

    // 2-2. 인가 / 인증
    INVALID_BEARER_GRANT_TYPE(false, 4200, "Bearer 타입이 아닙니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND(false, 4201, "JWT 토큰이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(false, 4202, "JWT 토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(false, 4203, "JWT 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED(false, 4204, "로그인 인증이 만료되었습니다. 다시 로그인을 진행해 주세요."),
    AUTHENTICATION_FAILED(false, 4205, "인증에 실패했습니다"),
    NOT_FOUND_USERDETAILS(false, 4206, "유저의 정보를 찾을 수 없습니다."),

    // 2-3. 펀딩
    UNAUTHORIZED_TO_ADD_LINK(false, 4300, "링크 추가 권한이 없습니다."),
    FUNDING_ITEM_ALREADY_EXISTS(false, 4301, "이미 등록된 펀딩 아이템입니다."),
    FUNDING_ITEM_PREVIEW_FAILED(false, 4302, "펀딩 아이템 미리보기에 실패했습니다."),
    FUNDING_ITEM_SAVE_FAILED(false, 4303, "펀딩 아이템 저장에 실패했습니다."),
    FUNDING_NOT_FOUND(false, 4304, "펀딩을 찾을 수 없습니다."),
    FUNDING_NOT_DELETED(false, 4305, "펀딩을 삭제할 수 없습니다."),
    UNAUTHORIZED_UPDATE_FUNDING(false, 4306, "펀딩 수정 권한이 없습니다."),
    UNAUTHORIZED_DELETE_FUNDING(false, 4307, "펀딩 삭제 권한이 없습니다."),
    UNAUTHORIZED_READ_FUNDING(false, 4308, "펀딩 조회 권한이 없습니다."),
    UNAUTHORIZED_FINISHED_FUNDING(false, 4309, "펀딩 종료 권한이 없습니다."),
    UNAUTHORIZED_TO_CREATE_FUNDING(false, 4310, "펀딩 등록 권한이 없습니다."),
    FUNDING_ITEM_LINK_FAILED(false, 4311, "펀딩 상품 등록에 실패했습니다."),
    FUNDING_CREATE_FAILED(false, 4312, "펀딩 등록에 실패했습니다."),
    UNAUTHORIZED_TO_GET_MY_FUNDING(false, 4313, "내 펀딩 조회 권한이 없습니다."),
    UNAUTHORIZED_TO_FINISH_FUNDING(false, 4314, "펀딩 종료 권한이 없습니다."),
    UNABLE_TO_ACQUIRE_ROCK(false, 4315, "락을 획득 할 수 없습니다."),
    UNABLE_TO_ACQUIRE_ROCK_INTERRUPT(false, 4316, "락을 획득하는 동안 문제가 발생했습니다."),
    FUNDING_ALREADY_EXISTS(false, 4317, "이미 진행중인 펀딩이 있습니다."),
    FUNDING_ITEM_NOT_FOUND(false, 4318, "링크 상품을 찾을 수 없습니다."),
    UNABLE_TO_GET_LINK_IMAGE(false, 4319, "링크 상품 이미지를 가져올 수 없습니다."),
    SERIALIZING_ERROR(false, 4320, "직열화하는 과정에서 오류가 발생했습니다."),
    DESERIALIZING_ERROR(false, 4321, "역직열화하는 과정에서 오류가 발생했습니다."),


    // 2-4. 후원
    DONATION_RANKING_DELIVERY_FAILED(false, 4400, "후원 랭킹 전달에 실패했습니다."),
    DONATION_READY_FAILED(false, 4401, "후원 결제 준비 요청에 실패했습니다."),
    DONATION_APPROVE_FAILED(false, 4402, "후원 결제 승인 요청에 실패했습니다."),

    // 2-5. 알림
    NOTIFICATION_NOT_FOUND(false, 4500, "알림을 찾을 수 없습니다."),
    NOTIFICATION_NOT_READ(false, 4501, "알림을 읽지 못했습니다."),
    NOTIFICATION_NOT_DELETED(false, 4502, "알림을 삭제할 수 없습니다."),
    UNAUTHORIZED_READ_NOTIFICATION(false, 4503, "알림 조회 권한이 없습니다."),
    UNAUTHORIZED_DELETE_NOTIFICATION(false, 4504, "알림 삭제 권한이 없습니다."),
    READ_NOTIFICATION_LIST_NOT_FOUND(false, 4505, "읽은 메시지가 없습니다"),

    /**
     * 3. 서버 에러(5000)
     */
    // 0. 공통
    INTERNAL_SERVER_ERROR(false, 5000, "서버 내부 에러가 발생했습니다."),
    UNEXPECTED_ERROR(false, 5001, "예상치 못한 에러가 발생했습니다."),
    FAIL_TO_ENCODING(false, 5002, "요청 인코딩에 실패했습니다."),
    FAIL_TO_JSON(false, 5003, "JSON 파싱 에러가 발생했습니다."),

    // 3-1. 이메일
    EMAIL_SEND_FAILED(false, 5100, "이메일 전송에 실패했습니다."),

    // 3-2. 알림
    NOTIFICATION_SEND_FAILED(false, 5200, "알림 전송에 실패했습니다."),
    UNAUTHORIZED_GET_NOTIFICATION(false, 5201, "알림을 읽을 권한이 업습니다.");

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
