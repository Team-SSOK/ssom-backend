package kr.ssok.ssom.backend.global.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GitHub Webhook 발신자 정보 DTO
 * GitHubWebhookUserDto와 동일한 구조이지만 명확성을 위해 별도 클래스로 정의
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GitHubWebhookSenderDto extends GitHubWebhookUserDto {
    
    // GitHubWebhookUserDto의 모든 필드를 상속받음
    // sender는 webhook 이벤트를 발생시킨 사용자를 의미
}
