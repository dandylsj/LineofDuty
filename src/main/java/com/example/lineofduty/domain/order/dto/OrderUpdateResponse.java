package com.example.lineofduty.domain.order.dto;

import com.example.lineofduty.domain.orderItem.OrderItem;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderUpdateResponse {
    private final Long orderItemId;
    private final Long productId;
    private final String productName;
    private final String productImageUrl;
    private final Long price;
    private final Long quantity;
    private final LocalDateTime createdAt;
    private final LocalDateTime modifiedAt;

    public OrderUpdateResponse(Long orderItemId, Long productId, String productName, String productImageUrl, Long price, Long quantity, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.price = price;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public static OrderUpdateResponse from(OrderItem orderItem) {
        return new OrderUpdateResponse(
                orderItem.getId(),
                orderItem.getProduct().getId(),
                orderItem.getProduct().getName(),
                orderItem.getProduct().getProductImageUrl(),
                orderItem.getProduct().getPrice(),
                orderItem.getQuantity(),
                orderItem.getCreatedAt(),
                orderItem.getModifiedAt()
        );
    }
}
