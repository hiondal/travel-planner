package com.travelplanner.place.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 장소 영업시간 엔티티.
 *
 * <p>places 테이블과 1:N 관계. 요일별 오픈/마감 시간을 관리한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(
    name = "place_business_hours",
    indexes = {
        @Index(name = "idx_place_business_hours_place_id", columnList = "place_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessHour {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_place_business_hours_place_id"))
    @JsonBackReference
    private Place place;

    /** 요일 (MONDAY ~ SUNDAY) */
    @Column(name = "day_of_week", nullable = false, length = 10)
    private String day;

    /** 오픈 시간 (HH:mm) */
    @Column(name = "open_time", length = 5)
    private String open;

    /** 마감 시간 (HH:mm) */
    @Column(name = "close_time", length = 5)
    private String close;

    /**
     * BusinessHour 생성 팩토리 메서드.
     *
     * @param place 장소 엔티티
     * @param day   요일
     * @param open  오픈 시간 (HH:mm)
     * @param close 마감 시간 (HH:mm)
     * @return 생성된 BusinessHour
     */
    public static BusinessHour of(Place place, String day, String open, String close) {
        BusinessHour businessHour = new BusinessHour();
        businessHour.place = place;
        businessHour.day = day;
        businessHour.open = open;
        businessHour.close = close;
        return businessHour;
    }

    /**
     * 주어진 시간이 영업시간 내에 있는지 확인한다.
     *
     * @param time 확인할 시각
     * @return 영업시간 내이면 true
     */
    public boolean isWithinHours(LocalTime time) {
        if (open == null || close == null) {
            return false;
        }
        try {
            LocalTime openTime = LocalTime.parse(open, TIME_FORMATTER);
            LocalTime closeTime = LocalTime.parse(close, TIME_FORMATTER);
            return !time.isBefore(openTime) && !time.isAfter(closeTime);
        } catch (Exception e) {
            return false;
        }
    }
}
