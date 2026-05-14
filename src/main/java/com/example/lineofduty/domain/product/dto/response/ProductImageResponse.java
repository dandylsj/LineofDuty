package com.example.lineofduty.domain.product.dto.response;

import com.example.lineofduty.domain.product.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductImageResponse {

    private Long id;
    private String imageUrl;
    private Integer orderIndex;

    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getOrderIndex()
        );
    }
}
