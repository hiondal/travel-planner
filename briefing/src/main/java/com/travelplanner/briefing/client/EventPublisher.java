package com.travelplanner.briefing.client;

import com.travelplanner.briefing.dto.internal.BriefingCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 도메인 이벤트 발행 컴포넌트.
 *
 * <p>Phase 1: 로그만 출력.</p>
 * <p>Phase 2: 실제 메시지 브로커(Kafka/RabbitMQ) 연동 예정.</p>
 */
@Slf4j
@Component
public class EventPublisher {

    /**
     * 브리핑 생성 이벤트를 발행한다.
     *
     * @param event 브리핑 생성 이벤트
     */
    public void publishBriefingCreated(BriefingCreatedEvent event) {
        log.info("[EVENT-PHASE1] BriefingCreatedEvent 발행 (로그 전용): briefingId={}, userId={}, type={}",
                event.getBriefingId(), event.getUserId(), event.getType());
        // Phase 2: 메시지 브로커에 이벤트 발행
    }
}
