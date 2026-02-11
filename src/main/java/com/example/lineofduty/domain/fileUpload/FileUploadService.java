package com.example.lineofduty.domain.fileUpload;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.ErrorMessage;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    private final List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "bmp");

    @Transactional
    public FileUploadResponse fileUpload(MultipartFile file) throws IOException {

        validateFile(file);

        LocalDateTime now = LocalDateTime.now();
        String fileName = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm/ss")) + "_" + UUID.randomUUID().toString();

        // S3에 업로드
        try (InputStream inputStream = file.getInputStream()) {
            s3Template.upload(bucketName, fileName, inputStream, ObjectMetadata.builder().contentType(file.getContentType()).build());
        }

        // 업로드된 파일의 URL 생성 (S3Template은 기본적으로 URL을 반환하지 않으므로 직접 구성하거나 S3Client 사용 필요)
        // 여기서는 S3Template의 편의 기능을 사용하여 업로드하고, URL은 표준 형식으로 조합합니다.
        // 실제 운영 환경에서는 CloudFront 등을 사용할 수도 있습니다.
        String publicUrl = s3Template.download(bucketName, fileName).getURL().toString();

        return new FileUploadResponse(fileName, publicUrl);

    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorMessage.FILE_IS_EMPTY);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new CustomException(ErrorMessage.INVALID_FILE_FORMAT);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new CustomException(ErrorMessage.INVALID_FILE_FORMAT);
        }
    }
}
