package com.example.lineofduty.domain.fileUpload;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileUploadService {

    // S3 기능 비활성화 (추후 스토리지 연동 시 복구)
    public FileUploadResponse fileUpload(MultipartFile file) throws IOException {
        throw new UnsupportedOperationException("파일 업로드 기능은 현재 비활성화 상태입니다.");
    }
}
