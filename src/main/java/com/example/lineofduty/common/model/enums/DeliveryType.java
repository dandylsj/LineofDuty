package com.example.lineofduty.common.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeliveryType {

    STANDARD("일반배송"),
    SAME_DAY("당일배송"),
    DAWN("새벽배송");

    private final String description;
}
