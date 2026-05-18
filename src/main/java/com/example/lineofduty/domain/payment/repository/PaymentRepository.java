package com.example.lineofduty.domain.payment.repository;

import com.example.lineofduty.domain.order.Order;
import com.example.lineofduty.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrder(Order order);

    Optional<Payment> findByOrder(Order order);

    Optional<Payment> findByPaymentKey(String paymentKey);
}
