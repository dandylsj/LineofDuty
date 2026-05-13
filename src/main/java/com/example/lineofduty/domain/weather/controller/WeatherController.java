package com.example.lineofduty.domain.weather.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.weather.dto.TodayWeatherResponse;
import com.example.lineofduty.domain.weather.dto.MidWeatherResponse;
import com.example.lineofduty.domain.weather.service.TodayWeatherService;
import com.example.lineofduty.domain.weather.service.MidWeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Weather", description = "날씨 정보 조회 API")
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final MidWeatherService weatherService;
    private final TodayWeatherService shortTermWeatherService;

    @Operation(summary = "중기 예보 조회", description = "훈련소 지역 등에 대한 중기(3~10일) 날씨 예보를 조회합니다.")
    @GetMapping("/mid-fcst")
    public ResponseEntity<GlobalResponse> getMidFcst(
            @RequestParam(defaultValue = "11B00000") String landRegId, // 육상예보 구역 (서울, 인천, 경기도)
            @RequestParam(defaultValue = "11B10101") String tempRegId  // 기온예보 구역 (서울)
    ) {
        MidWeatherResponse.Item response = weatherService.getMidFcst(landRegId, tempRegId);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.WEATHER_READ_SUCCESS, response));
    }

    @Operation(summary = "오늘의 날씨 조회", description = "기본 설정(예: 논산 훈련소) 위치의 당일 날씨 예보를 단기 조회합니다.")
    @GetMapping("/today")
    public ResponseEntity<GlobalResponse> getTodayWeather(
            @RequestParam(defaultValue = "36") int nx, // 논산 훈련소 기준 X 좌표
            @RequestParam(defaultValue = "127") int ny // 논산 훈련소 기준 Y 좌표
    ) {
        TodayWeatherResponse response = shortTermWeatherService.getTodayWeather(nx, ny);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.WEATHER_READ_SUCCESS, response));
    }
}
