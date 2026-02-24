package com.travelplanner.monitor.service;

import com.travelplanner.monitor.client.GoogleDirectionsClient;
import com.travelplanner.monitor.client.GooglePlacesClient;
import com.travelplanner.monitor.client.OpenWeatherMapClient;
import com.travelplanner.monitor.domain.*;
import com.travelplanner.monitor.dto.internal.PlaceStatusChangedEvent;
import com.travelplanner.monitor.repository.CollectedDataRepository;
import com.travelplanner.monitor.repository.MonitoringRepository;
import com.travelplanner.monitor.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 데이터 수집 서비스 구현체.
 *
 * <p>외부 API(Google Places, OpenWeatherMap, Google Directions)에서 상태 데이터를 수집하고
 * 4단계 상태 판정을 수행한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DataCollectionServiceImpl implements DataCollectionService {

    private final MonitoringRepository monitoringRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final CollectedDataRepository collectedDataRepository;
    private final GooglePlacesClient googlePlacesClient;
    private final OpenWeatherMapClient weatherClient;
    private final GoogleDirectionsClient directionsClient;
    private final StatusJudgmentService statusJudgmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, String> mntrRedisTemplate;

    @Value("${monitor.collection.window-hours:2}")
    private int collectionWindowHours;

    @Value("${monitor.collection.max-consecutive-failures:3}")
    private int maxConsecutiveFailures;

    private static final String BADGE_KEY_PREFIX = "mntr:badge:";
    private static final long BADGE_TTL_MINUTES = 5;

    @Override
    public CollectionJob triggerCollection(String triggeredBy, LocalDateTime triggeredAt) {
        LocalDateTime now = triggeredAt != null ? triggeredAt : LocalDateTime.now();
        List<MonitoringTarget> targets =
            monitoringRepository.findUpcomingTargets(now, now.plusHours(collectionWindowHours));

        log.info("수집 트리거 시작 - 대상 수: {}, by: {}", targets.size(), triggeredBy);

        String jobId = "job_" + UUID.randomUUID().toString().replace("-", "");

        for (MonitoringTarget target : targets) {
            try {
                collectForTarget(target);
            } catch (Exception e) {
                log.error("수집 실패 - placeId: {}, 오류: {}", target.getPlaceId(), e.getMessage());
            }
        }

        return new CollectionJob(jobId, "ACCEPTED", targets.size(), now);
    }

    @Override
    public void collectForTarget(MonitoringTarget target) {
        log.debug("수집 시작 - placeId: {}", target.getPlaceId());

        BusinessStatusData businessStatus =
            googlePlacesClient.getBusinessStatus(target.getPlaceId());
        WeatherData weatherData =
            weatherClient.getForecast(target.getLat(), target.getLng());
        TravelTimeData travelTimeData =
            directionsClient.getTravelTime(0, 0, target.getLat(), target.getLng());
        Integer popularity = googlePlacesClient.getCurrentPopularity(target.getPlaceId());

        CollectedData collectedData = buildCollectedData(
            target, businessStatus, weatherData, travelTimeData, popularity);
        collectedDataRepository.save(collectedData);

        PlaceStatusEnum prevStatus = target.getCurrentStatus();
        PlaceStatusEnum newStatus = statusJudgmentService.judge(
            target, businessStatus, weatherData, travelTimeData, popularity);

        String reason = statusJudgmentService.buildReason(businessStatus, weatherData, popularity);

        if (newStatus != PlaceStatusEnum.GREY) {
            target.resetFailure();
        }
        target.updateStatus(newStatus);
        monitoringRepository.save(target);

        String historyId = "sh_" + UUID.randomUUID().toString().replace("-", "");
        StatusHistory history = new StatusHistory(
            historyId, target.getPlaceId(), target.getScheduleItemId(),
            newStatus, reason, LocalDateTime.now());
        statusHistoryRepository.save(history);

        updateBadgeCache(target.getPlaceId(), newStatus);

        if (prevStatus != newStatus) {
            log.info("상태 변경 감지 - placeId: {}, {} -> {}",
                target.getPlaceId(), prevStatus, newStatus);
            eventPublisher.publishEvent(new PlaceStatusChangedEvent(
                target.getPlaceId(), prevStatus, newStatus, LocalDateTime.now()));
        }
    }

    @Override
    public void registerTarget(String placeId, String tripId, String scheduleItemId,
                                String userId, LocalDateTime visitDatetime,
                                double lat, double lng) {
        if (monitoringRepository.findByScheduleItemId(scheduleItemId).isPresent()) {
            log.debug("이미 등록된 모니터링 대상 - scheduleItemId: {}", scheduleItemId);
            return;
        }

        String id = "mt_" + UUID.randomUUID().toString().replace("-", "");
        MonitoringTarget target = new MonitoringTarget(
            id, placeId, tripId, scheduleItemId, userId, visitDatetime, lat, lng);
        monitoringRepository.save(target);
        log.info("모니터링 대상 등록 - placeId: {}, scheduleItemId: {}", placeId, scheduleItemId);
    }

    @Override
    public void unregisterTarget(String scheduleItemId) {
        monitoringRepository.findByScheduleItemId(scheduleItemId)
            .ifPresent(target -> {
                monitoringRepository.delete(target);
                log.info("모니터링 대상 해제 - scheduleItemId: {}", scheduleItemId);
            });
    }

    private CollectedData buildCollectedData(MonitoringTarget target,
                                              BusinessStatusData businessStatus,
                                              WeatherData weatherData,
                                              TravelTimeData travelTimeData,
                                              Integer popularity) {
        String id = "cd_" + UUID.randomUUID().toString().replace("-", "");
        CollectedData cd = new CollectedData(id, target.getPlaceId(), LocalDateTime.now());

        try {
            java.lang.reflect.Field f;

            f = CollectedData.class.getDeclaredField("businessStatus");
            f.setAccessible(true);
            f.set(cd, businessStatus != null ? businessStatus.getStatus() : null);

            f = CollectedData.class.getDeclaredField("precipitationProb");
            f.setAccessible(true);
            f.set(cd, weatherData != null ? weatherData.getPrecipitationProb() : null);

            f = CollectedData.class.getDeclaredField("weatherCondition");
            f.setAccessible(true);
            f.set(cd, weatherData != null ? weatherData.getCondition() : null);

            f = CollectedData.class.getDeclaredField("walkingMinutes");
            f.setAccessible(true);
            f.set(cd, travelTimeData != null ? travelTimeData.getWalkingMinutes() : null);

            f = CollectedData.class.getDeclaredField("transitMinutes");
            f.setAccessible(true);
            f.set(cd, travelTimeData != null ? travelTimeData.getTransitMinutes() : null);

            f = CollectedData.class.getDeclaredField("distanceM");
            f.setAccessible(true);
            f.set(cd, travelTimeData != null ? travelTimeData.getDistanceM() : null);

            if (popularity != null) {
                String congestionLevel = popularity >= 80 ? "VERY_CROWDED"
                    : popularity >= 50 ? "CROWDED" : "NORMAL";
                f = CollectedData.class.getDeclaredField("congestionLevel");
                f.setAccessible(true);
                f.set(cd, congestionLevel);
            }

        } catch (Exception e) {
            log.warn("CollectedData 필드 설정 실패: {}", e.getMessage());
        }

        return cd;
    }

    private void updateBadgeCache(String placeId, PlaceStatusEnum status) {
        try {
            String cacheKey = BADGE_KEY_PREFIX + placeId;
            StatusBadge badge = new StatusBadge(placeId, status, LocalDateTime.now());

            mntrRedisTemplate.opsForHash().put(cacheKey, "status", status.name());
            mntrRedisTemplate.opsForHash().put(cacheKey, "icon", badge.getIcon().name());
            mntrRedisTemplate.opsForHash().put(cacheKey, "colorHex", badge.getColorHex());
            mntrRedisTemplate.opsForHash().put(cacheKey, "updatedAt",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (badge.getLabel() != null) {
                mntrRedisTemplate.opsForHash().put(cacheKey, "label", badge.getLabel());
            }
            mntrRedisTemplate.expire(cacheKey, BADGE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("배지 캐시 갱신 실패 - placeId: {}, 오류: {}", placeId, e.getMessage());
        }
    }
}
