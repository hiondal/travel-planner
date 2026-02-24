package com.travelplanner.schedule.service;

import com.travelplanner.schedule.domain.Trip;
import com.travelplanner.schedule.dto.internal.ScheduleResult;
import com.travelplanner.schedule.dto.request.CreateTripRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * 여행 서비스 인터페이스.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface TripService {

    List<Trip> getTrips(String userId);

    Trip createTrip(String userId, CreateTripRequest request);

    Trip getTrip(String tripId, String userId);

    ScheduleResult getSchedule(String tripId, String userId);

    ScheduleResult getSchedule(String tripId, String userId, LocalDate date);

    void deleteTrip(String tripId, String userId);
}
