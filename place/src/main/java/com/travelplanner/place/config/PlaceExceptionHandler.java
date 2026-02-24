package com.travelplanner.place.config;

import com.travelplanner.common.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * PLACE 서비스 추가 예외 핸들러.
 *
 * <p>공통 GlobalExceptionHandler에서 처리하지 않는 Spring MVC 예외를 처리한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@RestControllerAdvice
public class PlaceExceptionHandler {

    /**
     * 필수 요청 파라미터 누락 예외를 처리한다.
     *
     * @param ex MissingServletRequestParameterException
     * @return 400 Bad Request
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        ErrorResponse body = ErrorResponse.of("BAD_REQUEST",
                "필수 파라미터가 없습니다: " + ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 요청 파라미터 타입 불일치 예외를 처리한다.
     *
     * @param ex MethodArgumentTypeMismatchException
     * @return 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse body = ErrorResponse.of("BAD_REQUEST",
                "파라미터 타입이 올바르지 않습니다: " + ex.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
