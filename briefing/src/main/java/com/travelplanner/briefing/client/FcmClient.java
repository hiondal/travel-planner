package com.travelplanner.briefing.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * FCM Push 발송 클라이언트.
 *
 * <p>Phase 1: 로그만 출력, 실제 FCM 미연동.</p>
 * <p>Phase 2: 실제 Firebase Admin SDK 연동 예정.</p>
 */
@Slf4j
@Component
public class FcmClient {

    /**
     * FCM Push 알림을 발송한다.
     *
     * <p>Phase 1에서는 로그만 출력한다.</p>
     *
     * @param userId 수신자 사용자 ID
     * @param title  알림 제목
     * @param body   알림 본문
     * @param data   추가 데이터
     */
    public void sendPush(String userId, String title, String body, Map<String, String> data) {
        log.info("[FCM-PHASE1] Push 발송 (로그 전용): userId={}, title={}, body={}, data={}",
                userId, title, body, data);
        // Phase 2에서 실제 FCM 연동 구현 예정
        // FirebaseMessaging.getInstance().send(Message.builder()...);
    }
}
