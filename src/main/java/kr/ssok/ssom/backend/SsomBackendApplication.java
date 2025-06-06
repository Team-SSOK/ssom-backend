package kr.ssok.ssom.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@EnableScheduling  // SSE 모니터링 스케줄러 활성화
@EnableAsync       // 비동기 처리 활성화
public class SsomBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsomBackendApplication.class, args);
    }

}
