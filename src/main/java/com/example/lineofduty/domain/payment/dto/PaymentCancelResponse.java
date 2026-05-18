package com.example.lineofduty.domain.payment.dto;

import com.example.lineofduty.domain.payment.Payment;
import com.example.lineofduty.domain.payment.PaymentStatus;
import lombok.Getter;

@Getter
public class PaymentCancelResponse {

    private final Long paymentId;
    private final String paymentKey;
    private final String orderNumber;
    private final String orderName;
    private final Long canceledPrice;
    private final PaymentStatus paymentStatus;

    public PaymentCancelResponse(Long paymentId, String paymentKey, String orderNumber, String orderName, Long totalPrice, PaymentStatus paymentStatus) {
        this.paymentId = paymentId;
        this.paymentKey = paymentKey;
        this.orderNumber = orderNumber;
        this.orderName = orderName;
        this.canceledPrice = totalPrice;
        this.paymentStatus = paymentStatus;
    }

    public static PaymentCancelResponse from(Payment payment) {
        return new PaymentCancelResponse(
                payment.getId(),
                payment.getPaymentKey(),
                payment.getOrder().getOrderNumber(),
                payment.getOrder().getOrderName(),
                payment.getTotalPrice(),
                payment.getStatus()
        );
    }

    public static PaymentCancelResponse canceled(Payment payment) {
        return new PaymentCancelResponse(
                payment.getId(),
                payment.getPaymentKey(),
                payment.getOrder().getOrderNumber(),
                payment.getOrder().getOrderName(),
                payment.getTotalPrice(),
                PaymentStatus.CANCELED
        );
    }
}
