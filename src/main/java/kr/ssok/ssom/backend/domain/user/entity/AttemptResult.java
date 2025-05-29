package kr.ssok.ssom.backend.domain.user.entity;

import lombok.Getter;

@Getter
public enum AttemptResult {
    SUCCESS("성공"),
    FAILED("실패"),
    BLOCKED("차단됨");

    private final String description;

    AttemptResult(String description) {
        this.description = description;
    }
}