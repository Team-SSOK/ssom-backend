package kr.ssok.ssom.backend.global.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    private static final String SCHEME = "http";
    private static final String HOST = "kudong.kr";
    private static final int PORT = 55034;

    /**
     * OpenSearchClient Bean 설정
     *
     * @return OpenSearchClient
     */
    @Bean
    public OpenSearchClient openSearchClient() {
        final HttpHost httpHost = new HttpHost(SCHEME, HOST, PORT);

        // OpenSearch와 통신하기 위한 OpenSearchTransport 객체를 생성
        final OpenSearchTransport transport =
                ApacheHttpClient5TransportBuilder.builder(httpHost)
                        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                                .setDefaultRequestConfig(RequestConfig.custom()
                                        .setConnectTimeout(Timeout.ofSeconds(10))    // 10초
                                        .setResponseTimeout(Timeout.ofSeconds(30))   // 30초
                                        .build())
                        ).build();

        return new OpenSearchClient(transport);
    }
}
