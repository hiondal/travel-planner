package com.travelplanner.alternative.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 대안 장소 후보.
 */
@Getter
@Setter
@NoArgsConstructor
public class PlaceCandidate {

    private String placeId;
    private String name;
    private int distanceM;
    private float rating;
    private double lat;
    private double lng;
    private String category;
    private boolean isOpen;
    private StatusBadge statusBadge;
    private String statusLabel;
    private String congestion;
    private Integer walkingMinutes;
    private Integer transitMinutes;
}
