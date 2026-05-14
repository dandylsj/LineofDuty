package com.example.lineofduty.domain.product.dto.request;

import com.example.lineofduty.common.model.enums.DeliveryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProductRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    @Min(value = 0)
    private Long price;

    @NotNull
    @Min(value = 0)
    private Long stock;

    private Long categoryId; // null 허용 - 카테고리 미지정 가능

    @Min(value = 0)
    private Long shippingFee; // null이면 기본값(0) 유지

    private Long freeShippingThreshold; // 무료배송 기준금액 (null이면 조건 없음)

    private DeliveryType deliveryType; // null이면 기본값(STANDARD) 유지
}
