package com.travelplanner.schedule.controller;

import com.travelplanner.common.response.ApiResponse;
import com.travelplanner.common.security.UserPrincipal;
import com.travelplanner.schedule.domain.Trip;
import com.travelplanner.schedule.dto.internal.ReplaceResult;
import com.travelplanner.schedule.dto.internal.ScheduleItemAddResult;
import com.travelplanner.schedule.dto.internal.ScheduleResult;
import com.travelplanner.schedule.dto.request.AddScheduleItemRequest;
import com.travelplanner.schedule.dto.request.CreateTripRequest;
import com.travelplanner.schedule.dto.request.ReplaceScheduleItemRequest;
import com.travelplanner.schedule.dto.response.*;
import com.travelplanner.schedule.service.ScheduleItemService;
import com.travelplanner.schedule.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Schedule 서비스 컨트롤러.
 *
 * <p>여행 목록/생성/조회, 일정 아이템 추가/삭제/교체 API를 제공한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Schedule", description = "여행 일정 관리 API")
public class SchdController {

    private final TripService tripService;
    private final ScheduleItemService scheduleItemService;

    /**
     * SCHD-00: 여행 목록 조회.
     */
    @GetMapping("/trips")
    @Operation(summary = "여행 목록 조회", description = "사용자의 여행 목록을 조회한다.")
    public ResponseEntity<ApiResponse<TripListResponse>> getTrips(
            @AuthenticationPrincipal UserPrincipal principal) {

        String userId = resolveUserId(principal);
        List<Trip> trips = tripService.getTrips(userId);

        List<TripResponse> tripResponses = trips.stream()
            .map(TripResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(TripListResponse.of(tripResponses)));
    }

    /**
     * SCHD-01: 여행 생성.
     */
    @PostMapping("/trips")
    @Operation(summary = "여행 생성", description = "새 여행 일정을 생성한다.")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @Valid @RequestBody CreateTripRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        String userId = resolveUserId(principal);
        Trip trip = tripService.createTrip(userId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created(TripResponse.from(trip)));
    }

    /**
     * SCHD-02: 여행 조회.
     */
    @GetMapping("/trips/{tripId}")
    @Operation(summary = "여행 조회", description = "특정 여행의 기본 정보를 조회한다.")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal) {

        String userId = resolveUserId(principal);
        Trip trip = tripService.getTrip(tripId, userId);
        return ResponseEntity.ok(ApiResponse.ok(TripResponse.from(trip)));
    }

    /**
     * SCHD-03: 일정표 조회.
     */
    @GetMapping("/trips/{tripId}/schedule")
    @Operation(summary = "일정표 조회", description = "여행 일정의 장소 목록을 시간순으로 조회한다.")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getSchedule(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal) {

        String userId = resolveUserId(principal);
        ScheduleResult result = tripService.getSchedule(tripId, userId);
        return ResponseEntity.ok(ApiResponse.ok(
            ScheduleResponse.from(result.getTrip(), result.getItems())));
    }

    /**
     * SCHD-04: 일정에 장소 추가.
     */
    @PostMapping("/trips/{tripId}/schedule-items")
    @Operation(summary = "장소 추가", description = "일정에 장소를 추가한다.")
    public ResponseEntity<?> addScheduleItem(
            @PathVariable String tripId,
            @Valid @RequestBody AddScheduleItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        String userId = resolveUserId(principal);
        ScheduleItemAddResult result =
            scheduleItemService.addScheduleItem(tripId, userId, request);

        if (result.isOutsideBusinessHours() && result.getItem() == null) {
            return ResponseEntity.ok(
                BusinessHoursWarningResponse.of(result.getBusinessHoursRange()));
        }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created(ScheduleItemResponse.from(result.getItem())));
    }

    /**
     * SCHD-05: 일정 장소 삭제.
     */
    @DeleteMapping("/trips/{tripId}/schedule-items/{itemId}")
    @Operation(summary = "장소 삭제", description = "일정표에서 장소를 삭제한다.")
    public ResponseEntity<Void> deleteScheduleItem(
            @PathVariable String tripId,
            @PathVariable String itemId,
            @AuthenticationPrincipal UserPrincipal principal) {

        String userId = resolveUserId(principal);
        scheduleItemService.deleteScheduleItem(tripId, itemId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * SCHD-06: 일정 장소 교체.
     */
    @PutMapping("/trips/{tripId}/schedule-items/{itemId}/replace")
    @Operation(summary = "장소 교체", description = "기존 장소를 대안 장소로 교체한다.")
    public ResponseEntity<ApiResponse<ReplaceScheduleItemResponse>> replaceScheduleItem(
            @PathVariable String tripId,
            @PathVariable String itemId,
            @Valid @RequestBody ReplaceScheduleItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        String userId = resolveUserId(principal);
        ReplaceResult result = scheduleItemService.replaceScheduleItem(
            tripId, itemId, request.getNewPlaceId(), userId);

        ReplaceScheduleItemResponse response = ReplaceScheduleItemResponse.builder()
            .scheduleItemId(result.getItem().getId())
            .originalPlace(PlaceRef.builder()
                .placeId(result.getOriginalPlaceId())
                .placeName(result.getOriginalPlaceName())
                .build())
            .newPlace(PlaceRef.builder()
                .placeId(result.getItem().getPlaceId())
                .placeName(result.getItem().getPlaceName())
                .build())
            .travelTimeDiffMinutes(result.getTravelTimeDiffMinutes())
            .updatedScheduleItems(
                result.getUpdatedItems().stream()
                    .map(ScheduleItemSummary::from)
                    .collect(Collectors.toList())
            )
            .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private String resolveUserId(UserPrincipal principal) {
        return principal != null ? principal.getUserId() : "anonymous";
    }
}
