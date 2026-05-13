package com.example.lineofduty.domain.fileUpload;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Tag(name = "File Upload", description = "파일 업로드 관련 API")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "파일 단건 업로드", description = "단일 파일을 서버에 업로드합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<GlobalResponse> fileUploadApi(@RequestParam("file") MultipartFile file) throws IOException {

        FileUploadResponse response = fileUploadService.fileUpload(file);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.FILE_UPLOAD_SUCCESS, response));
    }
}
