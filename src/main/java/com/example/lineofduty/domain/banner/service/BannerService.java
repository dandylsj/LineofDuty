package com.example.lineofduty.domain.banner.service;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.ErrorMessage;
import com.example.lineofduty.domain.banner.dto.BannerRequest;
import com.example.lineofduty.domain.banner.dto.BannerResponse;
import com.example.lineofduty.domain.banner.entity.Banner;
import com.example.lineofduty.domain.banner.repository.BannerRepository;
import com.example.lineofduty.domain.fileUpload.FileUploadResponse;
import com.example.lineofduty.domain.fileUpload.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerService {

    private final BannerRepository bannerRepository;
    private final FileUploadService fileUploadService;

    // 공개 - 활성 배너만 순서대로
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByOrderIndexAsc()
                .stream()
                .map(BannerResponse::from)
                .collect(Collectors.toList());
    }

    // 관리자 - 전체 배너
    public List<BannerResponse> getAllBanners() {
        return bannerRepository.findAllByOrderByOrderIndexAsc()
                .stream()
                .map(BannerResponse::from)
                .collect(Collectors.toList());
    }

    // 배너 생성
    @Transactional
    public BannerResponse createBanner(BannerRequest request, MultipartFile image) {
        String imageUrl = uploadIfPresent(image);

        Banner banner = Banner.builder()
                .badge(request.getBadge())
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .ctaText(request.getCtaText())
                .ctaPath(request.getCtaPath())
                .imageUrl(imageUrl)
                .accentColor(request.getAccentColor())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return BannerResponse.from(bannerRepository.save(banner));
    }

    // 배너 수정
    @Transactional
    public BannerResponse updateBanner(Long bannerId, BannerRequest request, MultipartFile image) {
        Banner banner = findById(bannerId);

        banner.update(
                request.getBadge(),
                request.getTitle(),
                request.getSubtitle(),
                request.getCtaText(),
                request.getCtaPath(),
                request.getAccentColor(),
                request.getOrderIndex(),
                request.getIsActive()
        );

        if (image != null && !image.isEmpty()) {
            banner.updateImageUrl(uploadIfPresent(image));
        }

        return BannerResponse.from(banner);
    }

    // 배너 삭제
    @Transactional
    public void deleteBanner(Long bannerId) {
        bannerRepository.delete(findById(bannerId));
    }

    private Banner findById(Long bannerId) {
        return bannerRepository.findById(bannerId)
                .orElseThrow(() -> new CustomException(ErrorMessage.BANNER_NOT_FOUND));
    }

    private String uploadIfPresent(MultipartFile image) {
        if (image == null || image.isEmpty()) return null;
        try {
            FileUploadResponse res = fileUploadService.fileUpload(image);
            return res.getUrl();
        } catch (IOException e) {
            throw new CustomException(ErrorMessage.FILE_UPLOAD_FAILED);
        }
    }
}
