package kr.ssok.ssom.backend.domain.user.service;

import kr.ssok.ssom.backend.domain.user.dto.SignupRequestDto;

public interface UserService {
    void registerUser(SignupRequestDto requestDto);
}
