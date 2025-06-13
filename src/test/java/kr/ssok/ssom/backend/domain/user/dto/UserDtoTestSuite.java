package kr.ssok.ssom.backend.domain.user.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * User DTO 계층 전체 테스트 슈트
 * 모든 User DTO 테스트 클래스를 포함하여 일괄 실행할 수 있습니다.
 */
@Suite
@Disabled("UserDtoTestSuite 임시 비활성화")
@SelectClasses({
        UserRequestDtoTest.class,
        UserResponseDtoTest.class,
        UserSearchResponseDtoTest.class,
        UserListResponseDtoTest.class,
        LoginRequestDtoTest.class,
        LoginResponseDtoTest.class,
        SignupRequestDtoTest.class,
        PasswordChangeRequestDtoTest.class,
        TokenRefreshRequestDtoTest.class,
        BiometricLoginRequestDtoTest.class,
        BiometricRegistrationRequestDtoTest.class,
        BiometricResponseDtoTest.class,
        BiometricStatusDtoTest.class
})
@DisplayName("User DTO 계층 테스트 슈트")
public class UserDtoTestSuite {
    // 테스트 슈트 클래스는 내용이 비어있어도 됩니다.
    // @Suite와 @SelectClasses 어노테이션이 테스트 실행을 담당합니다.
}
