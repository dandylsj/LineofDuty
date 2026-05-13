package com.example.lineofduty.domain.category.dto;

import com.fasterxml.jackson.core.JsonToken;
import lombok.Getter;

@Getter
public class CategoryCreateResponse {

    private final Long id;
    private final String name;
    private final String description;

    public CategoryCreateResponse(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
