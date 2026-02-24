package com.travelplanner.monitor.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.monitor.domain.WeatherData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * OpenWeatherMap API 클라이언트.
 *
 * <p>날씨 예보 데이터를 조회한다. API 키가 없으면 Mock 데이터를 반환한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenWeatherMapClient {

    private final RestTemplate restTemplate;

    @Value("${openweather.api-key:}")
    private String apiKey;

    @Value("${openweather.base-url:https://api.openweathermap.org/data/2.5}")
    private String baseUrl;

    /**
     * 날씨 예보를 조회한다.
     *
     * @param lat 위도
     * @param lng 경도
     * @return 날씨 데이터
     */
    public WeatherData getForecast(double lat, double lng) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("OpenWeatherMap API 키 없음 - Mock 데이터 반환 (lat={}, lng={})", lat, lng);
            return mockWeatherData();
        }

        try {
            String url = baseUrl + "/forecast?lat=" + lat + "&lon=" + lng
                + "&appid=" + apiKey + "&units=metric";
            WeatherForecastResponse response =
                restTemplate.getForObject(url, WeatherForecastResponse.class);

            if (response != null && response.getList() != null && !response.getList().isEmpty()) {
                WeatherForecastResponse.ForecastItem item = response.getList().get(0);
                int precipProb = item.getPop() != null
                    ? (int) (item.getPop() * 100) : 0;
                String condition = item.getWeather() != null && !item.getWeather().isEmpty()
                    ? item.getWeather().get(0).getMain() : "Clear";
                return new WeatherData(precipProb, condition, false);
            }
        } catch (RestClientException e) {
            log.warn("OpenWeatherMap API 호출 실패: {}", e.getMessage());
        }

        return WeatherData.unknown();
    }

    private WeatherData mockWeatherData() {
        return new WeatherData(10, "Clear", false);
    }

    @Getter
    private static class WeatherForecastResponse {
        private List<ForecastItem> list;

        @Getter
        private static class ForecastItem {
            private Double pop;
            private List<WeatherItem> weather;
        }

        @Getter
        private static class WeatherItem {
            @JsonProperty("main")
            private String main;
        }
    }
}
