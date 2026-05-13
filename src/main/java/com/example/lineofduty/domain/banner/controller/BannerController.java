package com.example.lineofduty.domain.banner.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.banner.dto.BannerRequest;
import com.example.lineofduty.domain.banner.dto.BannerResponse;
import com.example.lineofduty.domain.banner.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Banner", description = "배너 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerService bannerService;

    @Operation(summary = "활성 배너 목록 조회", description = "현재 활성화된 배너 목록을 조회합니다. (인증 불필요)")
    @GetMapping
    public ResponseEntity<GlobalResponse> getActiveBanners() {
        List<BannerResponse> banners = bannerService.getActiveBanners();
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.BANNER_LIST_SUCCESS, banners));
    }

    @Operation(summary = "전체 배너 목록 조회 (관리자)", description = "관리자가 모든 배너의 목록을 조회합니다.")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> getAllBanners() {
        List<BannerResponse> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.BANNER_LIST_SUCCESS, banners));
    }

    @Operation(summary = "배너 생성 (관리자)", description = "관리자가 새로운 배너를 생성합니다. (이미지 포함)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> createBanner(
            @RequestPart("data") BannerRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        BannerResponse response = bannerService.createBanner(request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(SuccessMessage.BANNER_CREATE_SUCCESS, response));
    }

    @Operation(summary = "배너 수정 (관리자)", description = "관리자가 기존 배너의 정보 또는 이미지를 수정합니다.")
    @PatchMapping(value = "/{bannerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> updateBanner(
            @PathVariable Long bannerId,
            @RequestPart("data") BannerRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        BannerResponse response = bannerService.updateBanner(bannerId, request, image);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.BANNER_UPDATE_SUCCESS, response));
    }

    @Operation(summary = "배너 삭제 (관리자)", description = "관리자가 특정 배너를 삭제합니다.")
    @DeleteMapping("/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> deleteBanner(@PathVariable Long bannerId) {
        bannerService.deleteBanner(bannerId);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.BANNER_DELETE_SUCCESS, null));
    }
}
