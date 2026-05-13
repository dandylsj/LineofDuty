package com.example.lineofduty.domain.enlistmentSchedule.controller;

import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.enlistmentSchedule.model.EnlistmentScheduleCreateRequest;
import com.example.lineofduty.domain.enlistmentSchedule.service.EnlistmentScheduleService;
import com.example.lineofduty.domain.user.dto.UserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.example.lineofduty.common.model.enums.SuccessMessage.*;

@Tag(name = "Enlistment Application", description = "입영 신청 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/enlistment-applications")
public class EnlistmentApplicationController {

    private final EnlistmentScheduleService enlistmentScheduleService;

    @Operation(summary = "입영 신청", description = "로그인한 사용자가 입영을 신청합니다.")
    @PostMapping
    public ResponseEntity<GlobalResponse> applyEnlistment(@AuthenticationPrincipal UserDetail userDetails, @RequestBody EnlistmentScheduleCreateRequest request) {
        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_APPLY_SUCCESS, enlistmentScheduleService.applyEnlistment(userDetails.getUser().getId(), request)));
    }

    @Operation(summary = "입영 신청 전체 목록 조회", description = "시스템에 등록된 전체 입영 신청 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse> getApplicationList() {
        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_LIST_SUCCESS, enlistmentScheduleService.getApplicationList()));
    }

    @Operation(summary = "내 입영 신청 단건 조회", description = "사용자의 특정 입영 신청 상세 내역을 조회합니다.")
    @GetMapping("/{applicationId}")
    public ResponseEntity<GlobalResponse> getApplication(@AuthenticationPrincipal UserDetail userDetails, @PathVariable Long applicationId) {
        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_LIST_SUCCESS, enlistmentScheduleService.getApplication(userDetails.getUser().getId(), applicationId)));
    }

    @Operation(summary = "입영 신청 취소", description = "사용자가 신청한 입영을 취소합니다.")
    @PatchMapping("/{applicationId}/cancel")
    public ResponseEntity<GlobalResponse> cancelApplication(@AuthenticationPrincipal UserDetail userDetails, @PathVariable Long applicationId) {
        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_CANCEL_SUCCESS, enlistmentScheduleService.cancelApplication(userDetails.getUser().getId(), applicationId)));
    }

}
