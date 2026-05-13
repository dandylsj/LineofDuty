package com.example.lineofduty.domain.user.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.user.dto.UserDetail;
import com.example.lineofduty.domain.user.dto.UserAdminResponse;
import com.example.lineofduty.domain.user.dto.UserWithdrawRequest;
import com.example.lineofduty.domain.user.dto.UserWithdrawResponse;
import com.example.lineofduty.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "User Admin", description = "사용자 관리자용 API")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    @Operation(summary = "전체 회원 목록 조회", description = "관리자가 전체 회원의 목록과 권한 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse> getAllUsers() {
        List<UserAdminResponse> responseList = userService.getAllUsers();
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.USER_ALL_READ_SUCCESS, responseList));
    }

    @Operation(summary = "회원 상세 정보 조회", description = "관리자가 특정 회원의 상세 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<GlobalResponse> getUser(@PathVariable Long userId) {
        UserAdminResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.USER_READ_SUCCESS, response));
    }

    @Operation(summary = "관리자 탈퇴", description = "관리자가 비밀번호를 확인하고 시스템에서 관리자 권한을 포함하여 탈퇴 처리합니다.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<GlobalResponse> withdrawAdmin(
            @AuthenticationPrincipal UserDetail userDetails,
            @RequestBody @Valid UserWithdrawRequest request
            ) {

        UserWithdrawResponse response = userService.withdrawAdmin(userDetails.getUser().getId(), request.getPassword());

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.USER_DELETE_ADMIN_SUCCESS, response));
    }
}
