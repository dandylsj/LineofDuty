package com.example.lineofduty.domain.order.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.order.dto.*;
import com.example.lineofduty.domain.order.service.OrderService;

import com.example.lineofduty.domain.user.dto.UserDetail;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping
    public ResponseEntity<GlobalResponse> createOrder(@Valid @RequestBody OrderCreateRequest request, @AuthenticationPrincipal UserDetail userDetail) {

        long userId = userDetail.getUser().getId();
        OrderCreateResponse response = orderService.createOrderService(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(SuccessMessage.ORDER_CREATE_SUCCESS, response));
    }

    // 주문 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<GlobalResponse> getOrder(@PathVariable Long orderId) {

        OrderGetResponse response = orderService.getOrderService(orderId);
        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.success(SuccessMessage.ORDER_GET_SUCCESS, response));
    }

    // 주문 수정 (이미지 포함)
    @PatchMapping(value = "/{orderId}/orderItems/{orderItemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse> updateOrder(
            @PathVariable Long orderId,
            @PathVariable Long orderItemId,
            @RequestParam Long productId,
            @RequestParam Long quantity,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        OrderUpdateResponse response = orderService.updateOrderService(orderId, orderItemId, productId, quantity, image);
        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.success(SuccessMessage.ORDER_UPDATE_SUCCESS, response));
    }

    // 주문 취소
    @DeleteMapping("/{orderId}")
    public ResponseEntity<GlobalResponse> deleteOrder(@PathVariable Long orderId, @AuthenticationPrincipal UserDetail userDetail) {

        Long userId = userDetail.getUser().getId();
        orderService.deleteOrderService(orderId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.successNodata(SuccessMessage.ORDER_DELETE_SUCCESS));
    }
}
