package kr.ssok.ssom.backend.domain.user.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class UserRequestDtoTest {

    @Test
    @DisplayName("UserRequestDto 인스턴스 생성 테스트")
    void createUserRequestDto() {
        // when
        UserRequestDto dto = new UserRequestDto();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto).isInstanceOf(UserRequestDto.class);
    }

    @Test
    @DisplayName("UserRequestDto 클래스 타입 확인 테스트")
    void testUserRequestDtoClassType() {
        // given
        UserRequestDto dto = new UserRequestDto();

        // when & then
        assertThat(dto.getClass()).isEqualTo(UserRequestDto.class);
        assertThat(dto.getClass().getSimpleName()).isEqualTo("UserRequestDto");
        assertThat(dto.getClass().getName()).isEqualTo("kr.ssok.ssom.backend.domain.user.dto.UserRequestDto");
    }
}
