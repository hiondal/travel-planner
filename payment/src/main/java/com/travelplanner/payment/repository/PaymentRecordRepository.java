package com.travelplanner.payment.repository;

import com.travelplanner.payment.domain.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 결제 이력 JPA 리포지토리.
 */
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, String> {

    Optional<PaymentRecord> findByTransactionId(String transactionId);
}
