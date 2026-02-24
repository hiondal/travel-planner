package com.travelplanner.monitor.service;

import com.travelplanner.monitor.domain.StatusBadge;
import com.travelplanner.monitor.dto.internal.StatusDetail;

import java.util.List;

/**
 * 배지 서비스 인터페이스.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface BadgeService {

    List<StatusBadge> getBadgeStatuses(List<String> placeIds);

    StatusDetail getStatusDetail(String placeId);
}
