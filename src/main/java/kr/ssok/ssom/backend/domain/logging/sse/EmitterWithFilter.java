package kr.ssok.ssom.backend.domain.logging.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class EmitterWithFilter {
    private final SseEmitter emitter;
    private final String appFilter;
    private final String levelFilter;

    public EmitterWithFilter(SseEmitter emitter, String appFilter, String levelFilter) {
        this.emitter = emitter;
        this.appFilter = appFilter;
        this.levelFilter = levelFilter;
    }

    public SseEmitter getEmitter() {
        return emitter;
    }

    public String getAppFilter() {
        return appFilter;
    }

    public String getLevelFilter() {
        return levelFilter;
    }
}
