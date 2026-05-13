package com.example.lineofduty.domain.banner.dto;

import com.example.lineofduty.domain.banner.entity.Banner;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BannerResponse {

    private Long id;
    private String badge;
    private String title;
    private String subtitle;
    private String ctaText;
    private String ctaPath;
    private String imageUrl;
    private String accentColor;
    private Integer orderIndex;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BannerResponse from(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .badge(banner.getBadge())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .ctaText(banner.getCtaText())
                .ctaPath(banner.getCtaPath())
                .imageUrl(banner.getImageUrl())
                .accentColor(banner.getAccentColor())
                .orderIndex(banner.getOrderIndex())
                .isActive(banner.getIsActive())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }
}
