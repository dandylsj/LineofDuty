package com.example.lineofduty.domain.product.service;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.ErrorMessage;
import com.example.lineofduty.domain.fileUpload.FileUploadService;
import com.example.lineofduty.domain.product.Product;
import com.example.lineofduty.domain.product.dto.response.ProductImageResponse;
import com.example.lineofduty.domain.product.entity.ProductImage;
import com.example.lineofduty.domain.product.repository.ProductImageRepository;
import com.example.lineofduty.domain.product.repository.ProductRepository;
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
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final FileUploadService fileUploadService;

    // 상세 이미지 추가
    @Transactional
    public ProductImageResponse addImage(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorMessage.PRODUCT_NOT_FOUND));

        String imageUrl = uploadFile(file);
        int nextOrder = productImageRepository.countByProductId(productId);

        ProductImage image = new ProductImage(product, imageUrl, nextOrder);
        return ProductImageResponse.from(productImageRepository.save(image));
    }

    // 상세 이미지 목록 조회
    public List<ProductImageResponse> getImages(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new CustomException(ErrorMessage.PRODUCT_NOT_FOUND);
        }
        return productImageRepository.findByProductIdOrderByOrderIndexAsc(productId)
                .stream()
                .map(ProductImageResponse::from)
                .collect(Collectors.toList());
    }

    // 상세 이미지 삭제
    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ErrorMessage.PRODUCT_IMAGE_NOT_FOUND));

        if (!image.getProduct().getId().equals(productId)) {
            throw new CustomException(ErrorMessage.NO_DELETE_PERMISSION);
        }

        productImageRepository.delete(image);
    }

    private String uploadFile(MultipartFile file) {
        try {
            return fileUploadService.fileUpload(file).getUrl();
        } catch (IOException e) {
            throw new CustomException(ErrorMessage.FILE_UPLOAD_FAILED);
        }
    }
}
