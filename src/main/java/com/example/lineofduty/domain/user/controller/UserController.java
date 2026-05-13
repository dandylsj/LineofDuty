package com.example.lineofduty.domain.user.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.fileUpload.FileUploadResponse;
import com.example.lineofduty.domain.fileUpload.FileUploadService;
import com.example.lineofduty.domain.user.dto.UserDetail;
import com.example.lineofduty.domain.user.dto.UserResponse;
import com.example.lineofduty.domain.user.dto.UserUpdateRequest;
import com.example.lineofduty.domain.user.dto.UserWithdrawRequest;
import com.example.lineofduty.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
class UserController {

    private final UserService userService;
    private final FileUploadService fileUploadService;

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<GlobalResponse> getMyProfile(@AuthenticationPrincipal UserDetail userDetails) {
        UserResponse response = userService.getMyProfile(userDetails.getUser().getId());
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.USER_READ_SUCCESS, response));
    }

    @Operation(summary = "내 프로필 수정", description = "로그인한 사용자의 프로필 정보를 수정합니다.")
    @PutMapping("/{userId}")
    public ResponseEntity<GlobalResponse> updateProfile(
            @AuthenticationPrincipal UserDetail userDetails,
            @RequestBody UserUpdateRequest requestDto) {
        UserResponse response = userService.updateProfile(userDetails.getUser().getId(), requestDto);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.USER_UPDATE_SUCCESS, response));
    }

    @Operation(summary = "프로필 이미지 업로드", description = "로그인한 사용자의 프로필 이미지를 업로드합니다.")
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse> uploadProfileImage(
            @AuthenticationPrincipal UserDetail userDetails,
            @RequestParam("file") MultipartFile file) throws IOException {

        FileUploadResponse fileResponse = fileUploadService.fileUpload(file);

        userService.updateProfileImage(userDetails.getUser().getId(), fileResponse.getUrl());

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.USER_UPDATE_SUCCESS, fileResponse));
    }

    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자가 비밀번호를 확인하고 탈퇴합니다.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<GlobalResponse> withdrawUser(
            @AuthenticationPrincipal UserDetail userDetails,
            @RequestBody @Valid UserWithdrawRequest request
            ) {

        userService.withdrawUser(userDetails.getUser().getId(), request.getPassword());

        return ResponseEntity.ok(GlobalResponse.successNodata(SuccessMessage.USER_DELETE_SUCCESS));
    }
}
