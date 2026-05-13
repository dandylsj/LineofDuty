package com.example.lineofduty.domain.enlistmentSchedule.controller;

import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.enlistmentSchedule.model.DefermentPatchRequest;
import com.example.lineofduty.domain.enlistmentSchedule.model.EnlistmentScheduleReadResponse;
import com.example.lineofduty.domain.enlistmentSchedule.service.EnlistmentScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.lineofduty.common.model.enums.SuccessMessage.*;
import static com.example.lineofduty.common.model.enums.SuccessMessage.DEFERMENTS_PROCEED;

@Tag(name = "Enlistment Admin", description = "입영 일정 및 신청 관리자용 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class EnlistmentAdminController {

    private final EnlistmentScheduleService enlistmentScheduleService;

    @Operation(summary = "입영 신청 승인", description = "관리자가 특정 입영 신청을 승인 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("enlistment-applications/{applicationId}/approve")
    public ResponseEntity<GlobalResponse> approveApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_APPROVE_SUCCESS, enlistmentScheduleService.approveApplication(applicationId)));
    }

    @Operation(summary = "입영 신청 일괄 승인", description = "관리자가 대기 중인 입영 신청을 일괄 승인 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("enlistment-applications/approve/bulk")
    public ResponseEntity<GlobalResponse> approveApplication() {
        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_APPROVE_SUCCESS, enlistmentScheduleService.approveApplicationBulk()));
    }

    @Operation(summary = "입영 연기 요청 목록 조회", description = "관리자가 입영 연기 요청 목록을 페이징하여 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/deferments")
    public ResponseEntity<GlobalResponse> getDefermentList(Pageable pageable) {
        return ResponseEntity.ok(GlobalResponse.success(DEFERMENTS_GET_SUCCESS, enlistmentScheduleService.getDefermentList(pageable)));
    }

    @Operation(summary = "입영 연기 요청 승인/반려", description = "관리자가 특정 입영 연기 요청을 승인 또는 반려합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/deferments/{defermentsId}")
    public ResponseEntity<GlobalResponse> processDeferment(
            @PathVariable Long defermentsId,
            @RequestBody DefermentPatchRequest request
    ) {

        return ResponseEntity.ok(GlobalResponse.success(DEFERMENTS_PROCEED,
                enlistmentScheduleService.processDeferment(
                        defermentsId,
                        request
                )
        ));
    }

    @Operation(summary = "입영 연기 요청 일괄 승인/반려", description = "관리자가 대기 중인 입영 연기 요청을 일괄 승인 또는 반려 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/deferments/bulk")
    public ResponseEntity<GlobalResponse> processDefermentBulk(@RequestBody DefermentPatchRequest request) {

        return ResponseEntity.ok(GlobalResponse.success(DEFERMENTS_PROCEED,
                        enlistmentScheduleService.processDefermentBulk(request.getDecisionStatus())
                )
        );
    }

    @Operation(summary = "연간 입영 일정 생성", description = "관리자가 새로운 연간 입영 일정을 생성합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/enlistment-schedule")
    public ResponseEntity<GlobalResponse> createEnlistmentSchedule() {
        List<EnlistmentScheduleReadResponse> data = enlistmentScheduleService.createEnlistmentYear();
        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_SUCCESS, data));
    }

}
