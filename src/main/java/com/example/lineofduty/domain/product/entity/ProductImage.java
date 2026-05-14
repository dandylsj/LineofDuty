package com.example.lineofduty.domain.product.entity;

import com.example.lineofduty.domain.product.Product;
import com.example.lineofduty.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor
public class ProductImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Integer orderIndex; // 이미지 순서

    public ProductImage(Product product, String imageUrl, Integer orderIndex) {
        this.product = product;
        this.imageUrl = imageUrl;
        this.orderIndex = orderIndex;
    }
}
