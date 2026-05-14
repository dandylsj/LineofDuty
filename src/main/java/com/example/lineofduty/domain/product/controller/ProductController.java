package com.example.lineofduty.domain.product.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.common.model.response.PageResponse;
import com.example.lineofduty.domain.fileUpload.FileUploadResponse;
import com.example.lineofduty.domain.fileUpload.FileUploadService;
import com.example.lineofduty.domain.product.dto.request.ProductRequest;
import com.example.lineofduty.domain.product.dto.response.ProductResponse;
import com.example.lineofduty.domain.product.dto.response.ProductImageResponse;
import com.example.lineofduty.domain.product.service.ProductImageService;
import com.example.lineofduty.domain.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Product", description = "상품 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;
    private final FileUploadService fileUploadService;

    @Operation(summary = "상품 등록", description = "관리자가 새로운 상품을 등록합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/products")
    public ResponseEntity<GlobalResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(SuccessMessage.PRODUCT_CREATE_SUCCESS, response));
    }

    @Operation(summary = "상품 단건 조회", description = "상품 ID를 통해 특정 상품의 상세 정보를 조회합니다.")
    @GetMapping("/products/{productId}")
    public ResponseEntity<GlobalResponse> getProduct(@PathVariable Long productId) {
        ProductResponse response = productService.getProduct(productId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(SuccessMessage.PRODUCT_GET_ONE_SUCCESS, response));
    }

    @Operation(summary = "상품 목록 조회", description = "등록된 상품들의 목록을 페이징하여 조회합니다. 키워드 검색 및 정렬이 가능합니다.")
    @GetMapping("/products")
    public ResponseEntity<GlobalResponse> getProDuctList(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sort, @RequestParam(defaultValue = "desc") String direction, @RequestParam(required = false) String keyword) {
        Page<ProductResponse> products = productService.getProductList(page, size, sort, direction, keyword);
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(SuccessMessage.PRODUCT_GET_ALL_SUCCESS, PageResponse.from(products)));
    }

    @Operation(summary = "상품 수정", description = "관리자가 상품 ID를 통해 특정 상품의 정보를 수정합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/products/{productId}")
    public ResponseEntity<GlobalResponse> updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(request, productId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(SuccessMessage.PRODUCT_UPDATE_SUCCESS, response));
    }

    @Operation(summary = "상품 이미지 업로드", description = "관리자가 특정 상품에 이미지를 업로드합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/admin/products/{productId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) throws IOException {

        FileUploadResponse fileResponse = fileUploadService.fileUpload(file);
        productService.updateProductImage(productId, fileResponse.getUrl());

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.PRODUCT_UPDATE_SUCCESS, fileResponse));
    }

    @Operation(summary = "상품 삭제", description = "관리자가 상품 ID를 통해 특정 상품을 삭제합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<GlobalResponse> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(GlobalResponse.successNodata(SuccessMessage.PRODUCT_DELETE_SUCCESS));
    }

    // ===================== 상세 이미지 API =====================

    @Operation(summary = "상품 상세 이미지 추가 (관리자)", description = "상품에 상세 이미지를 추가합니다. 여러 장 업로드 시 반복 호출하세요.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/admin/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse> addProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        ProductImageResponse response = productImageService.addImage(productId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(SuccessMessage.PRODUCT_IMAGE_ADD_SUCCESS, response));
    }

    @Operation(summary = "상품 상세 이미지 목록 조회", description = "상품의 상세 이미지 목록을 순서대로 조회합니다.")
    @GetMapping("/products/{productId}/images")
    public ResponseEntity<GlobalResponse> getProductImages(@PathVariable Long productId) {
        List<ProductImageResponse> response = productImageService.getImages(productId);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.PRODUCT_IMAGE_LIST_SUCCESS, response));
    }

    @Operation(summary = "상품 상세 이미지 삭제 (관리자)", description = "상품의 특정 상세 이미지를 삭제합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/products/{productId}/images/{imageId}")
    public ResponseEntity<GlobalResponse> deleteProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productImageService.deleteImage(productId, imageId);
        return ResponseEntity.ok(GlobalResponse.successNodata(SuccessMessage.PRODUCT_IMAGE_DELETE_SUCCESS));
    }
}
