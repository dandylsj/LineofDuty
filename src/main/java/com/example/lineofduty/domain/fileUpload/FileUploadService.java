package com.example.lineofduty.domain.fileUpload;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileUploadService {

    private final MinioClient minioClient;
    private final String bucket;
    private final String publicEndpoint;

    public FileUploadService(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.public-endpoint}") String publicEndpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.bucket}") String bucket) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.publicEndpoint = publicEndpoint;
        this.bucket = bucket;
    }

    public FileUploadResponse fileUpload(MultipartFile file) throws IOException {
        try {
            // 버킷이 없으면 자동 생성
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            String fileUrl = publicEndpoint + "/" + bucket + "/" + fileName;
            return new FileUploadResponse(fileName, fileUrl);

        } catch (Exception e) {
            throw new IOException("파일 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
