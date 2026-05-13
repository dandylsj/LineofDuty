package com.example.lineofduty.domain.kakao;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.token.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "Kakao Login", description = "카카오 소셜 로그인 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class KakaoController {

    private final KakaoService kakaoService;

    @Value("${frontend.url:}")
    private String frontendUrl;

    @Operation(summary = "카카오 로그인 콜백", description = "카카오 로그인 성공 후 인증 코드를 받아 처리하며, 성공 시 토큰을 반환하거나 프론트엔드로 리다이렉트합니다.")
    @GetMapping("/callback")
    public ResponseEntity<?> kakaoLogin(@RequestParam("code") String code, HttpServletResponse response) throws IOException {

        TokenResponse token = kakaoService.kakaoLogin(code);

        if (frontendUrl != null && !frontendUrl.isBlank()) {
            String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;

            String accessToken = java.net.URLEncoder.encode(token.getAccessToken(), java.nio.charset.StandardCharsets.UTF_8);
            String refreshToken = java.net.URLEncoder.encode(token.getRefreshToken(), java.nio.charset.StandardCharsets.UTF_8);

            String redirectUrl = base + "/oauth/kakao/callback"
                    + "#accessToken=" + accessToken
                    + "&refreshToken=" + refreshToken;

            response.sendRedirect(redirectUrl);
            return null;
        }

        // frontendUrl 미설정 시 JSON으로 토큰 반환
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.LOGIN_SUCCESS, token));
    }
}
