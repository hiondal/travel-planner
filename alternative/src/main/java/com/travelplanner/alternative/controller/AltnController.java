package com.travelplanner.alternative.controller;

import com.travelplanner.alternative.dto.internal.AlternativeSearchResult;
import com.travelplanner.alternative.dto.internal.SelectResult;
import com.travelplanner.alternative.dto.request.AlternativeSearchRequest;
import com.travelplanner.alternative.dto.request.SelectAlternativeRequest;
import com.travelplanner.alternative.dto.response.AlternativeSearchResponse;
import com.travelplanner.alternative.dto.response.PaywallResponse;
import com.travelplanner.alternative.dto.response.SelectAlternativeResponse;
import com.travelplanner.alternative.service.AlternativeService;
import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.common.response.ApiResponse;
import com.travelplanner.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 대안 REST API 컨트롤러.
 *
 * <p>대안 장소 검색 및 카드 선택 엔드포인트를 제공한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Tag(name = "Alternatives", description = "대안 장소 검색 및 카드 선택")
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AltnController {

    private final AlternativeService alternativeService;

    /**
     * 대안 장소를 검색하여 카드 3장을 생성한다.
     *
     * @param request   검색 요청
     * @param principal 인증 사용자
     * @return 대안 카드 목록 또는 Paywall 응답
     */
    @Operation(summary = "대안 장소 검색 (대안 카드 3장 생성)", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대안 카드 반환",
            content = @Content(schema = @Schema(implementation = AlternativeSearchResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "402", description = "구독 필요 (Free 티어)",
            content = @Content(schema = @Schema(implementation = PaywallResponse.class)))
    })
    @PostMapping("/alternatives/search")
    public ResponseEntity<?> searchAlternatives(
            @Valid @RequestBody AlternativeSearchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Paywall 체크 — Free 티어는 대안 카드 사용 불가
        if (principal != null && principal.getTier() == SubscriptionTier.FREE) {
            log.info("Paywall: Free 티어 사용자 대안 검색 차단: userId={}", principal.getUserId());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(new PaywallResponse("대안 카드는 Trip Pass/Pro 전용 기능입니다.", "/subscriptions/plans"));
        }

        String userId = principal != null ? principal.getUserId() : "anonymous";
        AlternativeSearchResult result = alternativeService.searchAlternatives(
                userId, request.getPlaceId(), request.getCategory(),
                request.getLocation().getLat(), request.getLocation().getLng());

        AlternativeSearchResponse response = AlternativeSearchResponse.of(
                request.getPlaceId(), result.getAlternatives(), result.getRadiusUsed());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 대안 카드를 선택하여 일정에 반영한다.
     *
     * @param altId     대안 카드 ID
     * @param request   선택 요청
     * @param principal 인증 사용자
     * @return 일정 반영 결과
     */
    @Operation(summary = "대안 카드 선택 및 일정 반영", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/alternatives/{altId}/select")
    public ResponseEntity<ApiResponse<SelectAlternativeResponse>> selectAlternative(
            @PathVariable String altId,
            @Valid @RequestBody SelectAlternativeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        String userId = principal != null ? principal.getUserId() : "anonymous";
        SelectResult result = alternativeService.selectAlternative(
                userId, altId, request.getOriginalPlaceId(),
                request.getScheduleItemId(), request.getTripId(),
                request.getSelectedRank(), request.getElapsedSeconds());

        SelectAlternativeResponse response = SelectAlternativeResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
