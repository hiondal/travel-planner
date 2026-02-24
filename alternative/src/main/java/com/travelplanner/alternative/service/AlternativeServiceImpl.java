package com.travelplanner.alternative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplanner.alternative.client.MonitorServiceClient;
import com.travelplanner.alternative.client.PlaceServiceClient;
import com.travelplanner.alternative.client.ScheduleServiceClient;
import com.travelplanner.alternative.domain.*;
import com.travelplanner.alternative.dto.internal.AlternativeSearchResult;
import com.travelplanner.alternative.dto.internal.ReplaceResult;
import com.travelplanner.alternative.dto.internal.SelectResult;
import com.travelplanner.alternative.repository.AlternativeCardSnapshotRepository;
import com.travelplanner.alternative.repository.AlternativeRepository;
import com.travelplanner.alternative.repository.SelectionLogRepository;
import com.travelplanner.alternative.scoring.ScoreCalculator;
import com.travelplanner.alternative.scoring.ScoreWeightsProvider;
import com.travelplanner.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 대안 서비스 구현체.
 *
 * <p>대안 장소 검색, 점수 계산, 카드 선택 처리를 담당한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlternativeServiceImpl implements AlternativeService {

    private static final int INITIAL_RADIUS = 1000;
    private static final int[] RADIUS_STEPS = {1000, 2000, 3000};
    private static final int MAX_CARDS = 3;
    private static final String CACHE_PREFIX = "altn:cards:";

    private final AlternativeRepository alternativeRepository;
    private final AlternativeCardSnapshotRepository snapshotRepository;
    private final SelectionLogRepository selectionLogRepository;
    private final PlaceServiceClient placeServiceClient;
    private final MonitorServiceClient monitorServiceClient;
    private final ScheduleServiceClient scheduleServiceClient;
    private final ScoreWeightsProvider scoreWeightsProvider;
    private final ScoreCalculator scoreCalculator;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AlternativeSearchResult searchAlternatives(String userId, String placeId,
                                                       String category, double lat, double lng) {
        log.info("대안 검색 시작: userId={}, placeId={}, category={}", userId, placeId, category);

        // 캐시 확인
        String cacheKey = buildCacheKey(placeId, category, INITIAL_RADIUS);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                AlternativeSearchResult result = objectMapper.readValue(cached, AlternativeSearchResult.class);
                log.debug("캐시 히트: cacheKey={}", cacheKey);
                return result;
            } catch (Exception e) {
                log.warn("캐시 역직렬화 실패, 새로 조회: error={}", e.getMessage());
            }
        }

        // 반경 단계별 검색
        int usedRadius = INITIAL_RADIUS;
        List<PlaceCandidate> validCandidates = new ArrayList<>();

        for (int radius : RADIUS_STEPS) {
            List<PlaceCandidate> candidates = placeServiceClient.searchNearby(lat, lng, category, radius);

            // 상태 배지 조회 및 필터링
            List<String> placeIds = candidates.stream()
                    .map(PlaceCandidate::getPlaceId)
                    .collect(Collectors.toList());
            Map<String, StatusBadge> badgeMap = monitorServiceClient.getBadges(placeIds);

            validCandidates = filterByStatus(candidates, badgeMap);
            usedRadius = radius;

            if (validCandidates.size() >= MAX_CARDS) break;
            log.debug("반경 {} m에서 {} 개 후보 확인, 반경 확장", radius, validCandidates.size());
        }

        // 점수 계산 및 상위 3개 선택
        WeightsContext weightsContext = new WeightsContext(userId, category,
                LocalDateTime.now().getHour() < 12 ? "MORNING" : "AFTERNOON");
        ScoreWeights weights = scoreWeightsProvider.getWeights(weightsContext);
        List<ScoredCandidate> scoredCandidates = scoreCalculator.calculateScores(validCandidates, weights);

        List<Alternative> alternatives = buildAlternativeCards(userId, placeId, scoredCandidates, weights);

        // DB 저장
        for (Alternative alt : alternatives) {
            alternativeRepository.save(alt);
        }

        // 스냅샷 저장 (ML 학습 데이터)
        saveCardSnapshots(userId, placeId, scoredCandidates.subList(0, alternatives.size()), weights);

        // 캐시 등록 (10분 TTL)
        cacheResults(cacheKey, alternatives, usedRadius);

        log.info("대안 검색 완료: {}개 카드 생성 (반경: {}m)", alternatives.size(), usedRadius);
        return new AlternativeSearchResult(alternatives, usedRadius);
    }

    @Override
    @Transactional
    public SelectResult selectAlternative(String userId, String altId, String originalPlaceId,
                                           String scheduleItemId, String tripId,
                                           int selectedRank, int elapsedSeconds) {
        log.info("대안 선택: userId={}, altId={}, scheduleItemId={}", userId, altId, scheduleItemId);

        Alternative alternative = alternativeRepository.findById(altId)
                .orElseThrow(() -> new ResourceNotFoundException("ALTERNATIVE", altId));

        // SCHD 서비스 일정 교체 (PUT /api/v1/trips/{tripId}/schedule-items/{itemId}/replace)
        ReplaceResult replaceResult = scheduleServiceClient.replaceScheduleItem(tripId, scheduleItemId, alternative.getPlaceId());

        // 선택 로그 저장
        SelectionLog selectionLog = SelectionLog.create(UUID.randomUUID().toString(),
                altId, userId, selectedRank, elapsedSeconds, replaceResult.isSuccess());
        selectionLogRepository.save(selectionLog);

        // 캐시 무효화
        invalidateCache(originalPlaceId);

        log.info("대안 선택 완료: altId={}, success={}", altId, replaceResult.isSuccess());
        return new SelectResult(scheduleItemId, originalPlaceId, "기존 장소",
                alternative.getPlaceId(), alternative.getName(),
                replaceResult.getTravelTimeDiffMinutes());
    }

    private List<PlaceCandidate> filterByStatus(List<PlaceCandidate> candidates,
                                                  Map<String, StatusBadge> badgeMap) {
        return candidates.stream()
                .filter(candidate -> {
                    StatusBadge badge = badgeMap.get(candidate.getPlaceId());
                    if (badge == null) return true;
                    // RED 상태는 제외
                    if ("RED".equals(badge.getStatus())) return false;
                    // YELLOW: 주의 필요 레이블 부여
                    if ("YELLOW".equals(badge.getStatus())) {
                        candidate.setStatusLabel("주의 필요");
                    }
                    // GREY: 정보 미확인 레이블 부여
                    if ("GREY".equals(badge.getStatus())) {
                        candidate.setStatusLabel("정보 미확인");
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<Alternative> buildAlternativeCards(String userId, String originalPlaceId,
                                                      List<ScoredCandidate> scored, ScoreWeights weights) {
        return scored.stream()
                .limit(MAX_CARDS)
                .map(s -> {
                    String reason = buildReason(s);
                    return Alternative.create(UUID.randomUUID().toString(), userId, originalPlaceId,
                            s.getCandidate(), reason, s.getTotalScore());
                })
                .collect(Collectors.toList());
    }

    private String buildReason(ScoredCandidate scored) {
        PlaceCandidate candidate = scored.getCandidate();
        if (scored.getDistanceScore() > 0.8) return "근거리 영업 중 동일 카테고리";
        if (scored.getRatingScore() > 0.8) return "높은 평점";
        if ("낮음".equals(candidate.getCongestion())) return "혼잡도 낮음";
        return "동일 카테고리 추천";
    }

    private String buildCacheKey(String placeId, String category, int radius) {
        return CACHE_PREFIX + placeId + ":" + category + ":" + radius;
    }

    private void invalidateCache(String placeId) {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + placeId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("캐시 무효화: placeId={}, {} 개 키 삭제", placeId, keys.size());
            }
        } catch (Exception e) {
            log.warn("캐시 무효화 실패: placeId={}, error={}", placeId, e.getMessage());
        }
    }

    private void cacheResults(String cacheKey, List<Alternative> alternatives, int radiusUsed) {
        try {
            AlternativeSearchResult result = new AlternativeSearchResult(alternatives, radiusUsed);
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(10));
        } catch (Exception e) {
            log.warn("캐시 저장 실패: cacheKey={}, error={}", cacheKey, e.getMessage());
        }
    }

    private void saveCardSnapshots(String userId, String placeId,
                                    List<ScoredCandidate> candidates, ScoreWeights weights) {
        try {
            String weightsJson = objectMapper.writeValueAsString(Map.of(
                    "distanceWeight", weights.getDistanceWeight(),
                    "ratingWeight", weights.getRatingWeight(),
                    "congestionWeight", weights.getCongestionWeight()
            ));
            for (ScoredCandidate sc : candidates) {
                String scoresJson = objectMapper.writeValueAsString(Map.of(
                        "distanceScore", sc.getDistanceScore(),
                        "ratingScore", sc.getRatingScore(),
                        "congestionScore", sc.getCongestionScore(),
                        "totalScore", sc.getTotalScore()
                ));
                AlternativeCardSnapshot snapshot = AlternativeCardSnapshot.create(
                        UUID.randomUUID().toString(), userId, placeId,
                        sc.getCandidate().getPlaceId(), weightsJson, scoresJson);
                snapshotRepository.save(snapshot);
            }
        } catch (JsonProcessingException e) {
            log.warn("카드 스냅샷 저장 실패: error={}", e.getMessage());
        }
    }
}
