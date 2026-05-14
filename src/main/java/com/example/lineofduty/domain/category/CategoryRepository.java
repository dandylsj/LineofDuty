package com.example.lineofduty.domain.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 최상위 카테고리만 조회 (parent가 null인 것)
    List<Category> findByParentIsNullOrderByIdAsc();
}
