package com.example.lineofduty.domain.category.dto;


import lombok.Getter;

@Getter
public class CategoryCreateRequest {

    private Long id;

    private String name;

    private String description;
}
