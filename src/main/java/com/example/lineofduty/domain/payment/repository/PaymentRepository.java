package com.example.lineofduty.domain.payment.repository;

import com.example.lineofduty.domain.order.Order;
import com.example.lineofduty.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrder(Order order);

    Optional<Payment> findByOrder(Order order);

    Optional<Payment> findByPaymentKey(String paymentKey);

    /**
     * 승인 대기(READY/ABORTED) 상태인 결제만 IN_PROGRESS로 바꾼다. 반환값이 0이면 이미 다른 요청이 승인 중이거나
     * 끝난 결제 - 같은 결제에 승인 요청이 동시에 두 번 와도(더블 클릭 등) 한 요청만 재고를 선점하게 한다.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Payment p SET p.status = com.example.lineofduty.domain.payment.PaymentStatus.IN_PROGRESS " +
            "WHERE p.id = :paymentId AND p.status IN (com.example.lineofduty.domain.payment.PaymentStatus.READY, " +
            "com.example.lineofduty.domain.payment.PaymentStatus.ABORTED)")
    int markInProgress(@Param("paymentId") Long paymentId);
}
