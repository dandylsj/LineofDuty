package com.example.lineofduty.domain.category.dto;

import com.example.lineofduty.domain.category.Category;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class CategoryCreateResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final Long parentId;
    private final List<CategoryCreateResponse> children;

    public CategoryCreateResponse(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.description = category.getDescription();
        this.parentId = category.getParent() != null ? category.getParent().getId() : null;
        this.children = category.getChildren().stream()
                .map(CategoryCreateResponse::new)
                .collect(Collectors.toList());
    }
}
