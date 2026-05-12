package com.example.lineofduty.domain.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProductRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    @Min(value = 0)
    private Long price;

    @NotNull
    @Min(value = 0)
    private Long stock;


}
