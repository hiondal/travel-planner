package com.travelplanner.place.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 장소 도메인 엔티티.
 *
 * <p>Google Places API에서 수집한 장소 정보를 저장한다.
 * {@code id}는 Google Place ID를 그대로 사용한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(
    name = "places",
    indexes = {
        @Index(name = "idx_places_city", columnList = "city"),
        @Index(name = "idx_places_category", columnList = "category"),
        @Index(name = "idx_places_city_category", columnList = "city, category"),
        @Index(name = "idx_places_coordinates", columnList = "lat, lng"),
        @Index(name = "idx_places_updated_at", columnList = "updated_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Place {

    /** Google Place ID */
    @Id
    @Column(name = "id", length = 200)
    private String id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 2000)
    private String address;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "rating", precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(name = "lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "photo_url", length = 2000)
    private String photoUrl;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<BusinessHour> businessHours = new ArrayList<>();

    /**
     * 장소 생성 팩토리 메서드.
     *
     * @param id       Google Place ID
     * @param name     장소명
     * @param address  주소
     * @param category 카테고리
     * @param rating   평점
     * @param lat      위도
     * @param lng      경도
     * @param timezone 타임존
     * @param photoUrl 대표 사진 URL
     * @param city     도시명
     * @return 생성된 Place
     */
    public static Place create(String id, String name, String address, String category,
                               BigDecimal rating, BigDecimal lat, BigDecimal lng,
                               String timezone, String photoUrl, String city) {
        Place place = new Place();
        place.id = id;
        place.name = name;
        place.address = address;
        place.category = category;
        place.rating = rating;
        place.lat = lat;
        place.lng = lng;
        place.timezone = timezone;
        place.photoUrl = photoUrl;
        place.city = city;
        place.updatedAt = LocalDateTime.now();
        return place;
    }

    /**
     * 장소 정보를 업데이트한다.
     *
     * @param name     장소명
     * @param address  주소
     * @param category 카테고리
     * @param rating   평점
     * @param timezone 타임존
     * @param photoUrl 대표 사진 URL
     * @param city     도시명
     */
    public void update(String name, String address, String category,
                       BigDecimal rating, String timezone, String photoUrl, String city) {
        this.name = name;
        this.address = address;
        this.category = category;
        this.rating = rating;
        this.timezone = timezone;
        this.photoUrl = photoUrl;
        this.city = city;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 영업시간을 교체한다 (기존 삭제 후 신규 추가).
     *
     * @param newHours 새 영업시간 목록
     */
    public void replaceBusinessHours(List<BusinessHour> newHours) {
        this.businessHours.clear();
        this.businessHours.addAll(newHours);
    }

    /**
     * 특정 요일의 영업시간을 반환한다.
     *
     * @param day 요일 (MONDAY ~ SUNDAY)
     * @return 해당 요일 영업시간 (없으면 empty)
     */
    public Optional<BusinessHour> getBusinessHoursForDay(String day) {
        return businessHours.stream()
                .filter(bh -> bh.getDay().equalsIgnoreCase(day))
                .findFirst();
    }

    /**
     * 주어진 일시에 영업 중인지 확인한다.
     *
     * @param dateTime 확인할 일시
     * @return 영업 중이면 true
     */
    public boolean isOpenAt(LocalDateTime dateTime) {
        String dayOfWeek = dateTime.getDayOfWeek().name();
        LocalTime time = dateTime.toLocalTime();
        return getBusinessHoursForDay(dayOfWeek)
                .map(bh -> bh.isWithinHours(time))
                .orElse(false);
    }

    /**
     * 위경도 좌표 값 객체를 반환한다.
     *
     * @return Coordinates
     */
    public Coordinates getCoordinates() {
        return new Coordinates(lat.doubleValue(), lng.doubleValue());
    }

    /**
     * 24시간 이상 미갱신 여부를 확인한다.
     *
     * @return 24시간 이상 미갱신이면 true
     */
    @JsonIgnore
    public boolean isStale() {
        return updatedAt.isBefore(LocalDateTime.now().minusHours(24));
    }
}
