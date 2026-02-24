package com.travelplanner.alternative.service;

import com.travelplanner.alternative.dto.internal.AlternativeSearchResult;
import com.travelplanner.alternative.dto.internal.SelectResult;

/**
 * 대안 서비스 인터페이스.
 */
public interface AlternativeService {

    AlternativeSearchResult searchAlternatives(String userId, String placeId,
                                                String category, double lat, double lng);

    SelectResult selectAlternative(String userId, String altId, String originalPlaceId,
                                   String scheduleItemId, String tripId,
                                   int selectedRank, int elapsedSeconds);
}
