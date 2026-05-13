package com.example.lineofduty.domain.banner.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.banner.dto.BannerRequest;
import com.example.lineofduty.domain.banner.dto.BannerResponse;
import com.example.lineofduty.domain.banner.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerService bannerService;

    // 공개 API - 활성 배너 목록 (인증 불필요)
    @GetMapping
    public ResponseEntity<GlobalResponse> getActiveBanners() {
        List<BannerResponse> banners = bannerService.getActiveBanners();
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.BANNER_LIST_SUCCESS, banners));
    }

    // 관리자 - 전체 배너 목록
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> getAllBanners() {
        List<BannerResponse> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.BANNER_LIST_SUCCESS, banners));
    }

    // 관리자 - 배너 생성 (이미지 포함)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> createBanner(
            @RequestPart("data") BannerRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        BannerResponse response = bannerService.createBanner(request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(SuccessMessage.BANNER_CREATE_SUCCESS, response));
    }

    // 관리자 - 배너 수정 (이미지 교체 포함)
    @PatchMapping(value = "/{bannerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> updateBanner(
            @PathVariable Long bannerId,
            @RequestPart("data") BannerRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        BannerResponse response = bannerService.updateBanner(bannerId, request, image);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.BANNER_UPDATE_SUCCESS, response));
    }

    // 관리자 - 배너 삭제
    @DeleteMapping("/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> deleteBanner(@PathVariable Long bannerId) {
        bannerService.deleteBanner(bannerId);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.BANNER_DELETE_SUCCESS, null));
    }
}
