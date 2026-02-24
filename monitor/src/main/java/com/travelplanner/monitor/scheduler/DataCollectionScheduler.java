package com.travelplanner.monitor.scheduler;

import com.travelplanner.monitor.service.DataCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 데이터 수집 스케줄러.
 *
 * <p>15분 주기로 방문 예정 2시간 전 대상 장소의 상태 데이터를 수집한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataCollectionScheduler {

    private final DataCollectionService dataCollectionService;

    /**
     * 15분 주기로 외부 API 수집을 실행한다.
     */
    @Scheduled(cron = "${monitor.collection.schedule-cron:0 */15 * * * *}")
    public void collectData() {
        log.info("모니터링 데이터 수집 스케줄러 실행 - {}", LocalDateTime.now());
        try {
            dataCollectionService.triggerCollection("scheduler", LocalDateTime.now());
        } catch (Exception e) {
            log.error("스케줄러 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
