package com.example.lineofduty.domain.category.dto;

import lombok.Getter;

@Getter
public class CategoryCreateRequest {

    private String name;
    private String description;
    private Long parentId; // null이면 최상위 카테고리, 값이 있으면 하위 카테고리
}
