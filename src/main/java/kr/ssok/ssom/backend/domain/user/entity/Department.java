package kr.ssok.ssom.backend.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum Department {
    CHANNEL(1, "CHN"), // 채널계 - 쏙앱
    CORE_BANK(2, "CORE"), // 계정계 - 쏙뱅크
    EXTERNAL(3, "EXT"), // 대외계 - 카프카, 프록시
    OPERATION(4, "OPR"); // 운영계 - 모니터링, devops

    private final int code;
    private final String prefix;

    // code로 enum 찾기
    public static Department fromCode(int code) {
        return Arrays.stream(values())
                .filter(dept -> dept.getCode() == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid department code: " + code));
    }

    // name으로 enum 찾기 (안전한 방식)
    public static Department fromName(String name) {
        try {
            return Department.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid department name: " + name);
        }
    }
}
