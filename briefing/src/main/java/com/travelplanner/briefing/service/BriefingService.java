package com.travelplanner.briefing.service;

import com.travelplanner.briefing.domain.Briefing;
import com.travelplanner.briefing.dto.internal.GenerateBriefingResult;
import com.travelplanner.briefing.dto.request.GenerateBriefingRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * 브리핑 서비스 인터페이스.
 */
public interface BriefingService {

    GenerateBriefingResult generateBriefing(GenerateBriefingRequest request);

    Briefing getBriefing(String briefingId, String userId);

    List<Briefing> getBriefingList(String userId, LocalDate date);
}
