package com.example.lineofduty.domain.dashboard;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.user.dto.UserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "관리자 대시보드 관련 API")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashBoardController {
    private final DashboardService dashboardService;

    @Operation(summary = "전체 요약 통계", description = "대시보드 메인 화면에 표시할 전체 요약 통계 데이터를 조회합니다.")
    @GetMapping("/summary")
    public ResponseEntity<GlobalResponse> summary(@AuthenticationPrincipal UserDetail userDetail) {
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.SUMMARY_SUCCESS, dashboardService.summary(userDetail)));
    }

    @Operation(summary = "입영 및 연기 요청 요약", description = "대기 중인 입영 요청 및 연기 요청 건수에 대한 요약 정보를 조회합니다.")
    @GetMapping("/requested")
    public ResponseEntity<GlobalResponse> summaryPending(@AuthenticationPrincipal UserDetail userDetail) {
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.SUMMARY_SUCCESS, dashboardService.summaryPending(userDetail)));
    }

    @Operation(summary = "연기 사유 통계 요약", description = "입영 연기 요청의 사유별 통계 요약 정보를 조회합니다.")
    @GetMapping("/deferments")
    public ResponseEntity<GlobalResponse> summaryDeferments(@AuthenticationPrincipal UserDetail userDetail) {
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.SUMMARY_SUCCESS, dashboardService.summaryDeferments(userDetail)));
    }

}
