package com.travelplanner.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PAYMENT 서비스 메인 애플리케이션.
 *
 * <p>구독 플랜 조회, IAP 영수증 검증, 구독 관리를 담당한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@SpringBootApplication
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
