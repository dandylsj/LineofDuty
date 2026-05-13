package com.example.lineofduty.domain.category;

import com.example.lineofduty.domain.category.dto.CategoryCreateRequest;
import com.example.lineofduty.domain.category.dto.CategoryCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 카테고리 생성
    @Transactional
    public CategoryCreateResponse createCategory(CategoryCreateRequest request) {

        Category category = new Category(
                request.getName(),
                request.getDescription()
        );

        Category savedCategory = categoryRepository.save(category);

        return new CategoryCreateResponse(savedCategory.getId(), savedCategory.getName(), savedCategory.getDescription());
    }



}
