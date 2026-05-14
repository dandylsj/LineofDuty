package com.example.lineofduty.domain.product.repository;

import com.example.lineofduty.domain.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByOrderIndexAsc(Long productId);

    int countByProductId(Long productId);
}
