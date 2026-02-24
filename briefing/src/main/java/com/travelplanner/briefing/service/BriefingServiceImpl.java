package com.travelplanner.briefing.service;

import com.travelplanner.briefing.client.EventPublisher;
import com.travelplanner.briefing.client.FcmClient;
import com.travelplanner.briefing.client.MonitorServiceClient;
import com.travelplanner.briefing.client.PayServiceClient;
import com.travelplanner.briefing.domain.*;
import com.travelplanner.briefing.dto.internal.BriefingCreatedEvent;
import com.travelplanner.briefing.dto.internal.GenerateBriefingResult;
import com.travelplanner.briefing.dto.internal.MonitorData;
import com.travelplanner.briefing.dto.internal.SubscriptionInfo;
import com.travelplanner.briefing.dto.request.GenerateBriefingRequest;
import com.travelplanner.briefing.generator.BriefingTextGenerator;
import com.travelplanner.briefing.repository.BriefingLogRepository;
import com.travelplanner.briefing.repository.BriefingRepository;
import com.travelplanner.common.enums.BriefingType;
import com.travelplanner.common.exception.BusinessException;
import com.travelplanner.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 브리핑 서비스 구현체.
 *
 * <p>출발 전 브리핑 생성, 조회, FCM 발송을 담당한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BriefingServiceImpl implements BriefingService {

    private final BriefingRepository briefingRepository;
    private final BriefingLogRepository briefingLogRepository;
    private final BriefingTextGenerator briefingTextGenerator;
    private final MonitorServiceClient monitorServiceClient;
    private final PayServiceClient payServiceClient;
    private final FcmClient fcmClient;
    private final EventPublisher eventPublisher;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${briefing.free-tier-daily-limit:1}")
    private int freeTierDailyLimit;

    private static final String CACHE_IDEM_PREFIX = "brif:idem:";
    private static final String CACHE_COUNT_PREFIX = "brif:count:";
    private static final String CACHE_BRIEFING_PREFIX = "brif:briefing:";
    private static final String CACHE_LIST_PREFIX = "brif:list:";

    @Override
    @Transactional
    public GenerateBriefingResult generateBriefing(GenerateBriefingRequest request) {
        String idempotencyKey = buildIdempotencyKey(request.getPlaceId(), request.getDepartureTime());
        log.info("브리핑 생성 시작: userId={}, placeId={}, idempotencyKey={}", request.getUserId(), request.getPlaceId(), idempotencyKey);

        // 멱등성 체크
        Optional<Briefing> existingByIdem = checkIdempotency(idempotencyKey);
        if (existingByIdem.isPresent()) {
            Briefing existing = existingByIdem.get();
            log.info("기존 브리핑 반환 (멱등성): briefingId={}", existing.getId());
            return GenerateBriefingResult.existing(existing.getId(), existing.getType());
        }

        // 구독 정보 및 무료 티어 제한 확인
        SubscriptionInfo subscriptionInfo = payServiceClient.getSubscriptionInfo(request.getUserId());
        boolean isFreeTier = "FREE".equals(subscriptionInfo.getTier());

        if (isFreeTier && !checkFreeTierLimit(request.getUserId())) {
            log.info("Free 티어 일일 브리핑 한도 초과: userId={}", request.getUserId());
            BriefingLog skippedLog = BriefingLog.skipped(UUID.randomUUID().toString(),
                    request.getUserId(), "FREE_TIER_LIMIT_EXCEEDED");
            briefingLogRepository.save(skippedLog);
            return GenerateBriefingResult.skipped();
        }

        // MNTR 서비스에서 최신 상태 조회
        MonitorData monitorData = monitorServiceClient.getLatestStatus(request.getPlaceId());

        // 상태 수준 판정 및 위험 항목 추출
        StatusLevel statusLevel = determineStatusLevel(monitorData.getOverallStatus());
        List<RiskItem> riskItems = buildRiskItems(monitorData, statusLevel);

        // 브리핑 타입 결정
        BriefingType type = determineBriefingType(statusLevel);

        // 브리핑 컨텍스트 생성 및 텍스트 생성
        BriefingContext context = new BriefingContext(monitorData, statusLevel, riskItems, subscriptionInfo.getTier());
        BriefingText briefingText = briefingTextGenerator.generate(context);

        // 브리핑 저장
        String briefingId = UUID.randomUUID().toString();
        BriefingContent content = new BriefingContent(
                monitorData.getBusinessStatus(),
                monitorData.getCongestion(),
                monitorData.getWeather(),
                monitorData.getWalkingMinutes(),
                monitorData.getTransitMinutes(),
                monitorData.getDistanceM()
        );

        String placeName = monitorData.getPlaceName() != null ? monitorData.getPlaceName() : request.getPlaceId();
        Briefing briefing = Briefing.create(briefingId, request.getUserId(), request.getScheduleItemId(),
                request.getPlaceId(), placeName, type, request.getDepartureTime(),
                idempotencyKey, briefingText.getText(), statusLevel, content, riskItems);

        briefingRepository.save(briefing);

        // 로그 저장
        BriefingLog createdLog = BriefingLog.created(UUID.randomUUID().toString(), briefingId, request.getUserId());
        briefingLogRepository.save(createdLog);

        // 캐시 갱신
        cacheIdempotencyKey(idempotencyKey, briefingId);
        incrementBriefingCount(request.getUserId());
        invalidateListCache(request.getUserId());

        // FCM Push 발송 (로그 전용)
        sendBriefingPush(request.getUserId(), briefing);

        // 이벤트 발행
        BriefingCreatedEvent event = new BriefingCreatedEvent(briefingId, request.getUserId(), request.getPlaceId(), type);
        eventPublisher.publishBriefingCreated(event);

        log.info("브리핑 생성 완료: briefingId={}, type={}", briefingId, type);
        return GenerateBriefingResult.created(briefingId, type);
    }

    @Override
    public Briefing getBriefing(String briefingId, String userId) {
        return briefingRepository.findByIdAndUserId(briefingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("BRIEFING", briefingId));
    }

    @Override
    public List<Briefing> getBriefingList(String userId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return briefingRepository.findByUserIdAndDate(userId, queryDate);
    }

    private Optional<Briefing> checkIdempotency(String idempotencyKey) {
        String cachedId = redisTemplate.opsForValue().get(CACHE_IDEM_PREFIX + idempotencyKey);
        if (cachedId != null) {
            return briefingRepository.findById(cachedId);
        }
        return briefingRepository.findByIdempotencyKey(idempotencyKey);
    }

    private boolean checkFreeTierLimit(String userId) {
        String countKey = CACHE_COUNT_PREFIX + userId + ":" + LocalDate.now();
        String countStr = redisTemplate.opsForValue().get(countKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count < freeTierDailyLimit) {
            return true;
        }
        // DB에서 재확인
        int dbCount = briefingRepository.countByUserIdAndCreatedAtDate(userId, LocalDate.now());
        return dbCount < freeTierDailyLimit;
    }

    private StatusLevel determineStatusLevel(String overallStatus) {
        if (overallStatus == null) return StatusLevel.SAFE;
        return switch (overallStatus.toUpperCase()) {
            case "DANGER", "RED" -> StatusLevel.DANGER;
            case "CAUTION", "YELLOW" -> StatusLevel.CAUTION;
            default -> StatusLevel.SAFE;
        };
    }

    private List<RiskItem> buildRiskItems(MonitorData monitorData, StatusLevel statusLevel) {
        List<RiskItem> items = new ArrayList<>();
        if (statusLevel == StatusLevel.SAFE) return items;

        if ("혼잡".equals(monitorData.getCongestion()) || "매우 혼잡".equals(monitorData.getCongestion())) {
            items.add(new RiskItem("혼잡도", "WARNING"));
        }
        if (monitorData.getPrecipitationProb() >= 70) {
            items.add(new RiskItem("날씨", "WARNING"));
        }
        if ("휴무".equals(monitorData.getBusinessStatus()) || "영업 종료".equals(monitorData.getBusinessStatus())) {
            items.add(new RiskItem("영업 상태", "DANGER"));
        }
        return items;
    }

    private BriefingType determineBriefingType(StatusLevel statusLevel) {
        return statusLevel == StatusLevel.SAFE ? BriefingType.SAFE : BriefingType.WARNING;
    }

    private String buildIdempotencyKey(String placeId, LocalDateTime departureTime) {
        // 출발 시간을 시간 단위로 정규화 (분/초 제거)
        String normalizedTime = departureTime.format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        return placeId + ":" + normalizedTime;
    }

    private void cacheIdempotencyKey(String idempotencyKey, String briefingId) {
        try {
            redisTemplate.opsForValue().set(CACHE_IDEM_PREFIX + idempotencyKey, briefingId, Duration.ofHours(2));
        } catch (Exception e) {
            log.warn("Redis 멱등성 캐시 저장 실패: key={}, error={}", idempotencyKey, e.getMessage());
        }
    }

    private void incrementBriefingCount(String userId) {
        String countKey = CACHE_COUNT_PREFIX + userId + ":" + LocalDate.now();
        try {
            redisTemplate.opsForValue().increment(countKey);
        } catch (Exception e) {
            log.warn("Redis 브리핑 카운트 증가 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    private void invalidateListCache(String userId) {
        String listKey = CACHE_LIST_PREFIX + userId + ":" + LocalDate.now();
        try {
            redisTemplate.delete(listKey);
        } catch (Exception e) {
            log.warn("Redis 목록 캐시 삭제 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    private void sendBriefingPush(String userId, Briefing briefing) {
        Map<String, String> data = new HashMap<>();
        data.put("briefing_id", briefing.getId());
        data.put("type", briefing.getType().name());
        data.put("place_id", briefing.getPlaceId());

        String title = briefing.isSafe() ? "[안심] " + briefing.getPlaceName() : "[주의] " + briefing.getPlaceName();
        fcmClient.sendPush(userId, title, briefing.getSummaryText(), data);
    }
}
