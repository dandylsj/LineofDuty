package com.example.lineofduty.domain.banner.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BannerRequest {
    private String badge;
    private String title;
    private String subtitle;
    private String ctaText;
    private String ctaPath;
    private String accentColor;
    private Integer orderIndex;
    private Boolean isActive;
}
