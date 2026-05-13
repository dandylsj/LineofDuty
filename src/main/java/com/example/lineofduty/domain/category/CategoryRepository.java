package com.example.lineofduty.domain.category;

import com.example.lineofduty.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
