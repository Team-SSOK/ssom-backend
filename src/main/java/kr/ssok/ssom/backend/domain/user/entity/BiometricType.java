package kr.ssok.ssom.backend.domain.user.entity;

import lombok.Getter;

@Getter
public enum BiometricType {
    FINGERPRINT("지문"),
    FACE("얼굴");

    private final String description;

    BiometricType(String description) {
        this.description = description;
    }
}