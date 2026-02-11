package com.example.lineofduty.domain.fileUpload;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.ErrorMessage;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);
    private final String firebaseBucket = "lineofdutyfileupload.firebasestorage.app";
    private final List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "bmp");

    @Transactional
    public FileUploadResponse fileUpload(MultipartFile file) throws IOException {
        log.info("Starting file upload: {}", file.getOriginalFilename());

        validateFile(file);

        LocalDateTime now = LocalDateTime.now();
        String fileName = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm/ss")) + "_" + UUID.randomUUID().toString();

        try {
            log.info("Getting bucket: {}", firebaseBucket);
            Bucket bucket = StorageClient.getInstance().bucket(firebaseBucket);
            
            if (bucket == null) {
                log.error("Bucket not found: {}", firebaseBucket);
                throw new CustomException(ErrorMessage.INVALID_REQUEST);
            }

            InputStream content = new ByteArrayInputStream(file.getBytes());

            log.info("Creating blob with name: {}", fileName);
            Blob blob = bucket.create(fileName, content, file.getContentType());
            log.info("Blob created successfully: {}", blob.getName());

            String publicUrl = String.format(
                    "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    firebaseBucket,
                    fileName.replace("/", "%2F")
            );

            return new FileUploadResponse(fileName, publicUrl);
        } catch (Exception e) {
            log.error("File upload failed", e);
            throw e;
        }
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
