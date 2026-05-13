package com.example.lineofduty.domain.banner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String badge;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 200)
    private String subtitle;

    @Column(length = 100)
    private String ctaText;

    @Column(length = 200)
    private String ctaPath;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 50)
    private String accentColor;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void update(String badge, String title, String subtitle,
                       String ctaText, String ctaPath,
                       String accentColor, Integer orderIndex, Boolean isActive) {
        this.badge = badge;
        this.title = title;
        this.subtitle = subtitle;
        this.ctaText = ctaText;
        this.ctaPath = ctaPath;
        this.accentColor = accentColor;
        this.orderIndex = orderIndex;
        this.isActive = isActive;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
