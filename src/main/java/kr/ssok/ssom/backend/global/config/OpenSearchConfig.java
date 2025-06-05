package kr.ssok.ssom.backend.global.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.scheme:http}")
    private String scheme;

    @Value("${opensearch.host:kudong.kr}")
    private String host;

    @Value("${opensearch.port:55034}")
    private int port;

    @Value("${opensearch.connect-timeout:10}")
    private int connectTimeoutSeconds;

    @Value("${opensearch.response-timeout:30}")
    private int responseTimeoutSeconds;

    /**
     * OpenSearchClient Bean 설정
     *
     * @return OpenSearchClient
     */
    @Bean
    public OpenSearchClient openSearchClient() {
        final HttpHost httpHost = new HttpHost(scheme, host, port);

        // OpenSearch와 통신하기 위한 OpenSearchTransport 객체를 생성
        final OpenSearchTransport transport =
                ApacheHttpClient5TransportBuilder.builder(httpHost)
                        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                                .setDefaultRequestConfig(RequestConfig.custom()
                                        .setConnectTimeout(Timeout.ofSeconds(connectTimeoutSeconds))
                                        .setResponseTimeout(Timeout.ofSeconds(responseTimeoutSeconds))
                                        .build())
                        ).build();

        return new OpenSearchClient(transport);
    }
}
