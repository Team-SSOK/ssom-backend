package kr.ssok.ssom.backend.domain.alert.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "AlertIssueRequestDto", description = "이슈 생성 완료 시 보내주는 포맷")
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertIssueRequestDto {

    private String action;
    private Issue issue;

    public String getAction() {
        return action;
    }

    public Issue getIssue() {
        return issue;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Issue {
        private Long number;

        private List<Assignee> assignees;

        @JsonProperty("created_at")
        private String createdAt;

        private List<Label> labels;

        public Long getNumber() { return number; }

        public List<Assignee> getAssignees() {
            return assignees;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public List<Label> getLabels() {
            return labels;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Assignee {
        private String login;

        public String getLogin() {
            return login;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Label {
        private String name;

        public String getName() {
            return name;
        }
    }
}