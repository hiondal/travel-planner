package com.travelplanner.monitor.controller;

import com.travelplanner.common.response.ApiResponse;
import com.travelplanner.common.security.UserPrincipal;
import com.travelplanner.monitor.domain.CollectionJob;
import com.travelplanner.monitor.domain.StatusBadge;
import com.travelplanner.monitor.dto.internal.StatusDetail;
import com.travelplanner.monitor.dto.request.CollectTriggerRequest;
import com.travelplanner.monitor.dto.response.BadgeListResponse;
import com.travelplanner.monitor.dto.response.CollectTriggerResponse;
import com.travelplanner.monitor.dto.response.StatusDetailResponse;
import com.travelplanner.monitor.service.BadgeService;
import com.travelplanner.monitor.service.DataCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Monitor 서비스 컨트롤러.
 *
 * <p>상태 배지 조회, 상태 상세 조회, 외부 데이터 수집 트리거 API를 제공한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Monitor", description = "장소 상태 모니터링 API")
public class MntrController {

    private final BadgeService badgeService;
    private final DataCollectionService dataCollectionService;

    /**
     * MNTR-01: 장소별 상태 배지 목록 조회.
     */
    @GetMapping("/badges")
    @Operation(summary = "상태 배지 목록 조회",
               description = "장소 ID 목록을 기준으로 각 장소의 상태 배지를 일괄 조회한다.")
    public ResponseEntity<ApiResponse<BadgeListResponse>> getBadges(
            @RequestParam("place_ids") String placeIds,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<String> placeIdList = parsePlaceIds(placeIds);
        List<StatusBadge> badges = badgeService.getBadgeStatuses(placeIdList);
        return ResponseEntity.ok(ApiResponse.ok(BadgeListResponse.of(badges)));
    }

    /**
     * MNTR-02: 장소 상태 상세 조회.
     */
    @GetMapping("/badges/{placeId}/detail")
    @Operation(summary = "장소 상태 상세 조회",
               description = "특정 장소의 영업상태, 혼잡도, 날씨, 이동시간 등 상세 상태를 조회한다.")
    public ResponseEntity<ApiResponse<StatusDetailResponse>> getStatusDetail(
            @PathVariable String placeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        StatusDetail detail = badgeService.getStatusDetail(placeId);
        return ResponseEntity.ok(ApiResponse.ok(StatusDetailResponse.from(detail)));
    }

    /**
     * 외부 데이터 수집 트리거 (내부 스케줄러 호출용).
     */
    @PostMapping("/monitor/collect")
    @Operation(summary = "외부 데이터 수집 트리거",
               description = "스케줄러가 15분 주기로 호출하여 외부 데이터를 수집한다.")
    public ResponseEntity<CollectTriggerResponse> triggerDataCollection(
            @RequestBody(required = false) CollectTriggerRequest request) {

        String triggeredBy = request != null ? request.getTriggeredBy() : "api";
        LocalDateTime triggeredAt = request != null ? request.getTriggeredAt() : LocalDateTime.now();

        CollectionJob job = dataCollectionService.triggerCollection(triggeredBy, triggeredAt);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(CollectTriggerResponse.from(job));
    }

    private List<String> parsePlaceIds(String placeIds) {
        return Arrays.stream(placeIds.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }
}
