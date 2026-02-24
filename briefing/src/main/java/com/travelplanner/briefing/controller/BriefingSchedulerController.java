package com.travelplanner.briefing.controller;

import com.travelplanner.briefing.dto.internal.GenerateBriefingResult;
import com.travelplanner.briefing.dto.request.GenerateBriefingRequest;
import com.travelplanner.briefing.dto.response.GenerateBriefingResponse;
import com.travelplanner.briefing.service.BriefingService;
import com.travelplanner.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 브리핑 생성 트리거 컨트롤러 (내부 스케줄러 호출용).
 *
 * <p>스케줄러가 출발 15~30분 전에 호출하여 브리핑을 생성한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Tag(name = "BriefingGeneration", description = "브리핑 생성 (내부/스케줄러 호출용)")
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BriefingSchedulerController {

    private final BriefingService briefingService;

    /**
     * 브리핑 생성을 트리거한다.
     *
     * <p>멱등성 키(placeId + departureTime 해시)로 중복 생성을 방지한다.</p>
     *
     * @param request 브리핑 생성 요청
     * @return 생성 결과
     */
    @Operation(summary = "브리핑 생성 트리거 (내부 스케줄러 호출용)")
    @PostMapping("/briefings/generate")
    public ResponseEntity<?> generateBriefing(@Valid @RequestBody GenerateBriefingRequest request) {
        log.info("브리핑 생성 요청: userId={}, placeId={}", request.getUserId(), request.getPlaceId());

        GenerateBriefingResult result = briefingService.generateBriefing(request);

        if (result.isSkipped()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(java.util.Map.of(
                            "status", "SKIPPED",
                            "reason", "FREE_TIER_LIMIT_EXCEEDED",
                            "message", "오늘의 브리핑을 모두 사용했습니다."
                    ));
        }

        GenerateBriefingResponse response = GenerateBriefingResponse.from(result);

        if (result.isCreated()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
