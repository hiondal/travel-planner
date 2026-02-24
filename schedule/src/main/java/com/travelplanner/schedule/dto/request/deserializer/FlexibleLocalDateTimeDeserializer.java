package com.travelplanner.schedule.dto.request.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * LocalDateTime 역직렬화기.
 *
 * <p>ISO-8601 오프셋 형식("2026-03-16T12:00:00+09:00")과
 * 로컬 형식("2026-03-16T12:00:00") 모두 지원한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            // offset 포함 형식 시도 (예: "2026-03-16T12:00:00+09:00")
            try {
                OffsetDateTime odt = OffsetDateTime.parse(value);
                return odt.toLocalDateTime();
            } catch (DateTimeParseException e2) {
                throw new IOException("날짜시간 파싱 실패: " + value, e2);
            }
        }
    }
}
