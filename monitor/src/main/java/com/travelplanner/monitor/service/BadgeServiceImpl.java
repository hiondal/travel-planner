package com.travelplanner.monitor.service;

import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.monitor.domain.*;
import com.travelplanner.monitor.dto.internal.StatusDetail;
import com.travelplanner.monitor.repository.MonitoringRepository;
import com.travelplanner.monitor.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 배지 서비스 구현체.
 *
 * <p>Redis Cache-Aside 패턴으로 배지 상태를 제공한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BadgeServiceImpl implements BadgeService {

    private final MonitoringRepository monitoringRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final RedisTemplate<String, String> mntrRedisTemplate;

    private static final String BADGE_KEY_PREFIX = "mntr:badge:";

    @Override
    public List<StatusBadge> getBadgeStatuses(List<String> placeIds) {
        return placeIds.stream()
            .map(this::getBadgeFromCacheOrDb)
            .collect(Collectors.toList());
    }

    @Override
    public StatusDetail getStatusDetail(String placeId) {
        MonitoringTarget target = monitoringRepository.findByPlaceId(placeId)
            .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND",
                "장소 상태 정보를 찾을 수 없습니다. placeId: " + placeId));

        PlaceStatusEnum status = target.getCurrentStatus();
        boolean showAlternative = status == PlaceStatusEnum.YELLOW || status == PlaceStatusEnum.RED;

        return StatusDetail.builder()
            .placeId(placeId)
            .placeName(placeId)
            .overallStatus(status)
            .businessStatus(new BusinessStatusData("OPEN", false))
            .weatherData(new WeatherData(0, "Clear", false))
            .travelTimeData(new TravelTimeData(0, null, 0, false))
            .congestionValue("정보 없음")
            .congestionUnknown(true)
            .reason(resolveReason(status))
            .showAlternativeButton(showAlternative)
            .updatedAt(target.getCurrentStatusUpdatedAt() != null
                ? target.getCurrentStatusUpdatedAt() : target.getCreatedAt())
            .build();
    }

    private StatusBadge getBadgeFromCacheOrDb(String placeId) {
        String cacheKey = BADGE_KEY_PREFIX + placeId;

        try {
            Map<Object, Object> cached = mntrRedisTemplate.opsForHash().entries(cacheKey);
            if (!cached.isEmpty()) {
                String statusStr = (String) cached.get("status");
                String updatedAtStr = (String) cached.get("updatedAt");
                if (statusStr != null) {
                    PlaceStatusEnum status = PlaceStatusEnum.valueOf(statusStr);
                    LocalDateTime updatedAt = updatedAtStr != null
                        ? LocalDateTime.parse(updatedAtStr) : LocalDateTime.now();
                    return new StatusBadge(placeId, status, updatedAt);
                }
            }
        } catch (Exception e) {
            log.warn("Redis 배지 캐시 조회 실패 - placeId: {}, 오류: {}", placeId, e.getMessage());
        }

        return monitoringRepository.findByPlaceId(placeId)
            .map(target -> {
                LocalDateTime updatedAt = target.getCurrentStatusUpdatedAt() != null
                    ? target.getCurrentStatusUpdatedAt() : target.getCreatedAt();
                return new StatusBadge(placeId, target.getCurrentStatus(), updatedAt);
            })
            .orElse(new StatusBadge(placeId, PlaceStatusEnum.GREY, LocalDateTime.now()));
    }

    private String resolveReason(PlaceStatusEnum status) {
        return switch (status) {
            case GREEN -> "정상";
            case YELLOW -> "주의 필요";
            case RED -> "경고";
            case GREY -> "데이터 미확인";
        };
    }
}
