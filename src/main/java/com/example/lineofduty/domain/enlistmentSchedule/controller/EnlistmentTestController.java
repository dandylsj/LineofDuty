package com.example.lineofduty.domain.enlistmentSchedule.controller;

import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.enlistmentSchedule.model.EnlistmentScheduleCreateRequest;
import com.example.lineofduty.domain.enlistmentSchedule.service.EnlistmentLockTestService;
import com.example.lineofduty.domain.enlistmentSchedule.service.EnlistmentScheduleRetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.lineofduty.common.model.enums.SuccessMessage.ENLISTMENT_APPLY_SUCCESS;

@Tag(name = "Enlistment Test", description = "입영 신청 동시성 테스트용 API")
@RestController
@RequestMapping("/api/test/enlistment")
@RequiredArgsConstructor
public class EnlistmentTestController {

    private final EnlistmentLockTestService enlistmentScheduleService;
    private final EnlistmentScheduleRetryService enlistmentScheduleRetryService;

    @Operation(summary = "입영 신청 (비관적 락)", description = "비관적 락을 적용한 입영 신청 동시성 테스트를 수행합니다.")
    @PostMapping("/pessimistic")
    public ResponseEntity<GlobalResponse> applyEnlistment(@RequestHeader("X-TEST-USER-ID") Long userId, @RequestBody EnlistmentScheduleCreateRequest request) {
        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_APPLY_SUCCESS, enlistmentScheduleService.applyEnlistmentTest(userId, request)));
    }

    @Operation(summary = "입영 신청 (낙관적 락)", description = "낙관적 락을 적용한 입영 신청 동시성 테스트를 수행합니다.")
    @PostMapping("/optimistic")
    public ResponseEntity<GlobalResponse> applyEnlistmentOptimisticLock(@RequestHeader("X-TEST-USER-ID") Long userId, @RequestBody EnlistmentScheduleCreateRequest request) {
        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_APPLY_SUCCESS, enlistmentScheduleRetryService.withdrawRetry(userId, request)));
    }

}
