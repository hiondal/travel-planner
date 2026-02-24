package com.travelplanner.monitor.service;

import com.travelplanner.monitor.domain.CollectionJob;
import com.travelplanner.monitor.domain.MonitoringTarget;

/**
 * 데이터 수집 서비스 인터페이스.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface DataCollectionService {

    CollectionJob triggerCollection(String triggeredBy, java.time.LocalDateTime triggeredAt);

    void collectForTarget(MonitoringTarget target);

    void registerTarget(String placeId, String tripId, String scheduleItemId,
                        String userId, java.time.LocalDateTime visitDatetime,
                        double lat, double lng);

    void unregisterTarget(String scheduleItemId);
}
