package com.example.lineofduty.domain.payment.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.payment.dto.*;
import com.example.lineofduty.domain.payment.service.PaymentService;
import com.example.lineofduty.domain.user.dto.UserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "결제 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 요청", description = "새로운 결제 요청을 생성합니다.")
    @PostMapping
    public ResponseEntity<GlobalResponse> createPayment(@Valid @RequestBody PaymentCreateRequest request, @AuthenticationPrincipal UserDetail userDetail) {

        long userId = userDetail.getUser().getId();
        PaymentCreateResponse response = paymentService.createPaymentService(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(SuccessMessage.PAYMENT_CREATE_SUCCESS, response));
    }

    @Operation(summary = "결제 승인", description = "결제 대행사(PG)를 통해 결제를 최종 승인합니다.")
    @PostMapping("/confirm")
    public ResponseEntity<GlobalResponse> confirmPayment(@Valid @RequestBody PaymentConfirmRequest request) {

        PaymentConfirmResponse response = paymentService.confirmPaymentService(request);
        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.success(SuccessMessage.PAYMENT_CONFIRM_SUCCESS, response));
    }

    @Operation(summary = "결제 조회 (paymentKey)", description = "결제 키(paymentKey)를 통해 결제 내역을 조회합니다.")
    @GetMapping("/{paymentKey}")
    public ResponseEntity<GlobalResponse> getPaymentByPaymentKey(@PathVariable String paymentKey) {

        PaymentGetResponse response = paymentService.getPaymentByPaymentKeyService(paymentKey);
        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.success(SuccessMessage.PAYMENT_GET_SUCCESS, response));
    }

    @Operation(summary = "결제 조회 (orderNumber)", description = "주문 번호(orderNumber)를 통해 결제 내역을 조회합니다.")
    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<GlobalResponse> getPaymentByOrderId(@PathVariable String orderNumber) {

        PaymentGetResponse response = paymentService.getPaymentByOrderIdService(orderNumber);
        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.success(SuccessMessage.PAYMENT_GET_SUCCESS, response));
    }

    @Operation(summary = "결제 취소", description = "결제 키(paymentKey)를 통해 결제를 취소(환불)합니다.")
    @PostMapping("/{paymentKey}/cancel")
    public ResponseEntity<GlobalResponse> cancelPayment(@Valid @RequestBody PaymentCancelRequest request, @PathVariable String paymentKey, @AuthenticationPrincipal UserDetail userDetail) {

        long userId = userDetail.getUser().getId();
        PaymentCancelResponse response = paymentService.cancelPaymentService(request, paymentKey, userId);
        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.success(SuccessMessage.PAYMENT_CANCEL_SUCCESS, response));
    }

}
