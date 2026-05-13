package com.example.lineofduty.domain.enlistmentSchedule.controller;
import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.dashboard.model.EnlistmentThisWeekResponse;
import com.example.lineofduty.domain.enlistmentSchedule.model.EnlistmentScheduleReadResponse;
import com.example.lineofduty.domain.enlistmentSchedule.model.ScheduleOfThisWeekResponse;
import com.example.lineofduty.domain.enlistmentSchedule.service.EnlistmentScheduleService;
import com.example.lineofduty.domain.weather.dto.TodayWeatherResponse;
import com.example.lineofduty.domain.weather.service.TodayWeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.example.lineofduty.common.model.enums.SuccessMessage.*;

@Tag(name = "Enlistment Schedule", description = "입영 일정 조회 관련 API")
@RestController
@RequestMapping("/api/enlistment")
@RequiredArgsConstructor
@Slf4j
public class EnlistmentScheduleController {

    private final EnlistmentScheduleService enlistmentScheduleService;
    private final TodayWeatherService shortTermWeatherService;

    @Operation(summary = "입영 가능 일정 전체 조회", description = "등록된 전체 입영 가능 일정을 페이징 처리하여 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse> getEnlistmentList(Pageable pageable) {

        List<EnlistmentScheduleReadResponse> list = enlistmentScheduleService.getEnlistmentList(pageable);

        Page<EnlistmentScheduleReadResponse> data = new PageImpl<>(list,pageable, list.size());

        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_SUCCESS, data));
    }

    @Operation(summary = "입영 일정 단건 조회", description = "입영 일정 ID를 통해 특정 입영 일정을 상세 조회합니다.")
    @GetMapping("/{scheduleId}")
    public ResponseEntity<GlobalResponse> getEnlistment(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_SUCCESS, enlistmentScheduleService.getEnlistment(scheduleId)));
    }

    @Operation(summary = "이번 주 입영일정 요약", description = "논산 훈련소 기준 날씨 정보와 이번 주 입영 일정 요약을 함께 조회합니다.")
    @GetMapping("/thisWeek")
    public ResponseEntity<GlobalResponse> summaryScheduleOfThisWeek(
            @RequestParam(defaultValue = "36") int nx, // 논산 훈련소 기준 X 좌표
            @RequestParam(defaultValue = "127") int ny // 논산 훈련소 기준 Y 좌표
    ) {

        TodayWeatherResponse weatherResponse = shortTermWeatherService.getTodayWeather(nx, ny);

        EnlistmentThisWeekResponse enlistmentResponse = enlistmentScheduleService.summaryScheduleOfThisWeek();

        ScheduleOfThisWeekResponse data = new ScheduleOfThisWeekResponse(weatherResponse, enlistmentResponse);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.SUMMARY_SUCCESS, data));
    }

    @Operation(summary = "기간별 입영 일정 검색", description = "시작일과 종료일을 지정하여 해당 기간 내의 입영 일정을 페이징 조회합니다.")
    @GetMapping("/search")
    public ResponseEntity<GlobalResponse> searchEnlistment(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                           Pageable pageable
    ) {

        List<EnlistmentScheduleReadResponse> list = enlistmentScheduleService.searchEnlistment(startDate, endDate, pageable);

        Page<EnlistmentScheduleReadResponse> data = new PageImpl<>(list,pageable, list.size());

        return ResponseEntity.ok(GlobalResponse.success(ENLISTMENT_LIST_SUCCESS, data));
    }
}
