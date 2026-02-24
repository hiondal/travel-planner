package com.travelplanner.place.client;

import com.travelplanner.place.domain.BusinessHour;
import com.travelplanner.place.domain.Place;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Places API 응답을 담는 내부 전송 객체.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class GooglePlaceDto {

    private String placeId;
    private String name;
    private String address;
    private Float rating;
    private List<BusinessHourData> businessHours = new ArrayList<>();
    private double lat;
    private double lng;
    private String timezone;
    private String photoUrl;
    private String category;

    public GooglePlaceDto() {
    }

    /**
     * Place 도메인 엔티티로 변환한다.
     *
     * @param city 도시명
     * @return Place 엔티티
     */
    public Place toPlace(String city) {
        Place place = Place.create(
                placeId, name, address, category,
                rating, lat, lng, timezone, photoUrl, city
        );
        List<BusinessHour> hours = new ArrayList<>();
        for (BusinessHourData data : businessHours) {
            hours.add(BusinessHour.of(place, data.getDay(), data.getOpen(), data.getClose()));
        }
        place.replaceBusinessHours(hours);
        return place;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Float getRating() {
        return rating;
    }

    public void setRating(Float rating) {
        this.rating = rating;
    }

    public List<BusinessHourData> getBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(List<BusinessHourData> businessHours) {
        this.businessHours = businessHours;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 영업시간 데이터 내부 클래스.
     */
    public static class BusinessHourData {
        private String day;
        private String open;
        private String close;

        public BusinessHourData() {
        }

        public BusinessHourData(String day, String open, String close) {
            this.day = day;
            this.open = open;
            this.close = close;
        }

        public String getDay() {
            return day;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public String getOpen() {
            return open;
        }

        public void setOpen(String open) {
            this.open = open;
        }

        public String getClose() {
            return close;
        }

        public void setClose(String close) {
            this.close = close;
        }
    }
}
