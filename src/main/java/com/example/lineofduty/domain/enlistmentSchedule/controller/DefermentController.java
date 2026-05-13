package com.example.lineofduty.domain.enlistmentSchedule.controller;

import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.enlistmentSchedule.model.DefermentsPostRequest;
import com.example.lineofduty.domain.enlistmentSchedule.service.EnlistmentScheduleService;
import com.example.lineofduty.domain.user.dto.UserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.example.lineofduty.common.model.enums.SuccessMessage.DEFERMENTS_GET_SUCCESS;
import static com.example.lineofduty.common.model.enums.SuccessMessage.DEFERMENTS_SUCCESS;

@Tag(name = "Deferment", description = "입영 연기 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deferments")
public class DefermentController {

    private final EnlistmentScheduleService enlistmentScheduleService;

    @Operation(summary = "입영 신청 연기", description = "로그인한 사용자가 신청한 입영을 연기 요청합니다.")
    @PostMapping
    public ResponseEntity<GlobalResponse> defermentsSchedule(@AuthenticationPrincipal UserDetail userDetails, @RequestBody DefermentsPostRequest request) {
        return ResponseEntity.ok(GlobalResponse.success(DEFERMENTS_SUCCESS, enlistmentScheduleService.defermentsSchedule(userDetails.getUser().getId(), request)));
    }

    @Operation(summary = "내 입영 연기 단건 조회", description = "사용자의 특정 입영 연기 신청 상세 내역을 조회합니다.")
    @GetMapping("/{defermentsId}")
    public ResponseEntity<GlobalResponse> getDeferment(@AuthenticationPrincipal UserDetail userDetails, @PathVariable Long defermentsId) {
        return ResponseEntity.ok(GlobalResponse.success(DEFERMENTS_GET_SUCCESS, enlistmentScheduleService.getDeferment(userDetails.getUser().getId(), defermentsId)));
    }

    @Operation(summary = "입영 연기 신청 목록 조회", description = "시스템에 등록된 전체 입영 연기 신청 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse> getDefermentList(Pageable pageable) {
        return ResponseEntity.ok(GlobalResponse.success(DEFERMENTS_GET_SUCCESS, enlistmentScheduleService.getDefermentList(pageable)));
    }

}
