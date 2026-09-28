package com.example.lineofduty.domain.product.repository;

import com.example.lineofduty.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 재고가 충분할 때만 원자적으로 차감한다. 반환값이 0이면 재고 부족.
     * 조건 검사와 차감이 한 문장이라 DB 행 잠금만으로 동시 요청이 정확히 처리된다(분산 락 불필요).
     * status를 stock보다 먼저 계산해야 차감 전 재고로 품절 여부를 판단한다(MySQL은 SET을 왼쪽부터 적용).
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Product p SET " +
            "p.status = CASE WHEN p.stock = :quantity " +
            "THEN com.example.lineofduty.common.model.enums.ProductStatus.SOLD_OUT ELSE p.status END, " +
            "p.stock = p.stock - :quantity " +
            "WHERE p.id = :productId AND p.stock >= :quantity")
    int decreaseStockIfAvailable(@Param("productId") Long productId, @Param("quantity") Long quantity);

    /** 원자적 재고 증가 (결제 취소/승인 실패 시 복구). 재고가 생기면 판매중으로 되돌린다 - Product.increaseStock과 같은 규칙. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Product p SET " +
            "p.status = CASE WHEN p.stock + :quantity > 0 " +
            "THEN com.example.lineofduty.common.model.enums.ProductStatus.ON_SALE ELSE p.status END, " +
            "p.stock = p.stock + :quantity " +
            "WHERE p.id = :productId")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Long quantity);
}
