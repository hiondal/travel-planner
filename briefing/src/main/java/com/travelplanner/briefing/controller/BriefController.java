package com.travelplanner.briefing.controller;

import com.travelplanner.briefing.domain.Briefing;
import com.travelplanner.briefing.dto.response.BriefingDetailResponse;
import com.travelplanner.briefing.dto.response.BriefingListResponse;
import com.travelplanner.briefing.service.BriefingService;
import com.travelplanner.common.response.ApiResponse;
import com.travelplanner.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 브리핑 REST API 컨트롤러.
 *
 * <p>브리핑 상세 조회 및 목록 조회 엔드포인트를 제공한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Tag(name = "Briefings", description = "브리핑 조회 및 목록")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BriefController {

    private final BriefingService briefingService;

    /**
     * 브리핑 상세를 조회한다.
     *
     * @param briefingId 브리핑 ID
     * @param principal  인증 사용자
     * @return 브리핑 상세 정보
     */
    @Operation(summary = "브리핑 상세 조회", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/briefings/{briefingId}")
    public ResponseEntity<ApiResponse<BriefingDetailResponse>> getBriefing(
            @PathVariable String briefingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Briefing briefing = briefingService.getBriefing(briefingId, principal.getUserId());
        boolean expired = briefing.isExpired();
        BriefingDetailResponse response = BriefingDetailResponse.from(briefing, expired);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 브리핑 목록을 조회한다.
     *
     * @param date      조회 날짜 (기본값: 오늘)
     * @param principal 인증 사용자
     * @return 브리핑 목록
     */
    @Operation(summary = "브리핑 목록 조회", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/briefings")
    public ResponseEntity<ApiResponse<BriefingListResponse>> getBriefingList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<Briefing> briefings = briefingService.getBriefingList(principal.getUserId(), queryDate);
        BriefingListResponse response = BriefingListResponse.of(queryDate, briefings);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
