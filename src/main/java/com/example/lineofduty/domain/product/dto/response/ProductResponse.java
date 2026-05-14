package com.example.lineofduty.domain.product.dto.response;

import com.example.lineofduty.common.model.enums.DeliveryType;
import com.example.lineofduty.common.model.enums.ProductStatus;
import com.example.lineofduty.domain.product.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public class ProductResponse {

    private Long productId;
    private String name;
    private String description;
    private Long price;
    private Long stock;
    private ProductStatus status;
    private String productImageUrl;
    private Long categoryId;
    private String categoryName;
    private Long shippingFee;
    private Long freeShippingThreshold;
    private DeliveryType deliveryType;
    private String detailContent;
    private List<ProductImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getProductImageUrl(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getShippingFee(),
                product.getFreeShippingThreshold(),
                product.getDeliveryType(),
                product.getDetailContent(),
                product.getImages().stream()
                        .map(ProductImageResponse::from)
                        .collect(Collectors.toList()),
                product.getCreatedAt(),
                product.getModifiedAt()
        );
    }
}
