package kr.ssok.ssom.backend.global.client;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import kr.ssok.ssom.backend.global.dto.FcmMessageRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Firebase 클라이언트
 */
@Slf4j
@Component
public class FirebaseClient {

    /**
     * FCM 알림 전송
     *
     * @param request FCM 알림 요청 DTO
     */
    public void sendNotification(FcmMessageRequestDto request) {
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(request.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(request.getTitle())
                            .setBody(request.getBody())
                            .build());

            // data 필드가 있으면 추가
            if (request.getData() != null && !request.getData().isEmpty()) {
                messageBuilder.putAllData(request.getData());
            }

            Message message = messageBuilder.build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("푸시 알림 전송 성공: {}", response);

        } catch (Exception e) {
            log.error("푸시 알림 전송 실패: {}", e.getMessage());
        }
    }
}
