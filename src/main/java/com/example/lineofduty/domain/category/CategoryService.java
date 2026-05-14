package com.example.lineofduty.domain.category;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.ErrorMessage;
import com.example.lineofduty.domain.category.dto.CategoryCreateRequest;
import com.example.lineofduty.domain.category.dto.CategoryCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 카테고리 생성
    @Transactional
    public CategoryCreateResponse createCategory(CategoryCreateRequest request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomException(ErrorMessage.CATEGORY_NOT_FOUND));
        }

        Category category = new Category(request.getName(), request.getDescription(), parent);
        return new CategoryCreateResponse(categoryRepository.save(category));
    }

    // 전체 카테고리 계층 구조 조회 (최상위 카테고리 + 하위 카테고리 포함)
    public List<CategoryCreateResponse> getAllCategories() {
        return categoryRepository.findByParentIsNullOrderByIdAsc()
                .stream()
                .map(CategoryCreateResponse::new)
                .collect(Collectors.toList());
    }

    // 특정 카테고리 단건 조회
    public CategoryCreateResponse getCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorMessage.CATEGORY_NOT_FOUND));
        return new CategoryCreateResponse(category);
    }

    // 카테고리 삭제
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorMessage.CATEGORY_NOT_FOUND));

        if (!category.getChildren().isEmpty()) {
            throw new CustomException(ErrorMessage.CATEGORY_HAS_CHILDREN);
        }

        categoryRepository.delete(category);
    }
}
