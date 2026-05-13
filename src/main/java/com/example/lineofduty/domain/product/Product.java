package com.example.lineofduty.domain.product;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.ErrorMessage;
import com.example.lineofduty.common.model.enums.ProductStatus;
import com.example.lineofduty.domain.category.Category;
import com.example.lineofduty.domain.product.dto.request.ProductRequest;
import com.example.lineofduty.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Long stock;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    public Product(String name, String description, Long price, Long stock, ProductStatus status) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.status = status;
    }

    public void updateStock(Long newStock) {
        this.stock = newStock;
        updateStatusBasedOnStock();
    }

    private void updateStatusBasedOnStock() {
        if (this.stock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        } else if (this.stock > 0 && this.status == ProductStatus.SOLD_OUT) {
            this.status = ProductStatus.ON_SALE;
        }
    }

    public void update(ProductRequest request) {
        if (request.getName() != null) this.name = request.getName();
        if (request.getDescription() != null) this.description = request.getDescription();
        if (request.getPrice() != null) this.price = request.getPrice();
        if (request.getStock() != null) {
            updateStock(request.getStock());
        }
    }

    public void decreaseStock(Long quantity) {
        if (this.stock < quantity) {
            throw new CustomException(ErrorMessage.OUT_OF_STOCK);
        }

        this.stock -= quantity;

        if (this.stock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        }
    }

    public void increaseStock(Long quantity) {
        this.stock += quantity;

        if (this.stock > 0) {
            this.status = ProductStatus.ON_SALE;
        }
    }

    public void updateProductImage(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }
}
