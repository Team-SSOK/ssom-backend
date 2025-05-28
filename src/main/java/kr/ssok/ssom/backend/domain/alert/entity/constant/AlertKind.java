package kr.ssok.ssom.backend.domain.alert.entity.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AlertKind {

    OPENSEARCH(1, "OpenSearch"),
    GRAFANA(2, "Grafana"),
    ISSUE(3, "Issue"),
    JENKINS(4, "Jenkins"),
    ARGOCD(5, "ArgoCD");

    private final int idx;
    private final String value;
}
