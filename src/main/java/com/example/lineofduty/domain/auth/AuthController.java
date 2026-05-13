package com.example.lineofduty.domain.auth;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.auth.dto.EmailVerificationRequest;
import com.example.lineofduty.domain.auth.dto.LoginRequest;
import com.example.lineofduty.domain.auth.dto.SignupRequest;
import com.example.lineofduty.domain.token.TokenRequest;
import com.example.lineofduty.domain.token.TokenResponse;
import com.example.lineofduty.domain.user.dto.UserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "인증 코드 이메일 발송", description = "회원가입을 위해 이메일로 인증 코드를 발송합니다.")
    @PostMapping("/email/send")
    public ResponseEntity<GlobalResponse> sendEmail(@RequestBody EmailVerificationRequest request) {
        authService.sendCode(request.getEmail());
        return ResponseEntity.ok(GlobalResponse.successNodata(SuccessMessage.SEND_AUTHENTICATION_CODE));
    }

    @Operation(summary = "이메일 인증 코드 확인", description = "발송된 인증 코드를 확인하여 이메일 인증을 완료합니다.")
    @PostMapping("/email/verify")
    public ResponseEntity<GlobalResponse> verifyEmail(@RequestBody EmailVerificationRequest request) {
        authService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(GlobalResponse.successNodata(SuccessMessage.EMAIL_VERIFICATION_SUCCESSFUL));
    }

    @Operation(summary = "회원가입", description = "이메일 인증 후 회원가입을 진행합니다.")
    @PostMapping("/signup")
    public ResponseEntity<GlobalResponse> signup(@Valid @RequestBody SignupRequest request) {

        authService.signup(request);

        return ResponseEntity.ok(GlobalResponse.successNodata(SuccessMessage.AUTH_SIGNUP_SUCCESS));
    }

    @Operation(summary = "로그인", description = "로그인 성공 시 Access Token과 Refresh Token을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<GlobalResponse> login(@Valid @RequestBody LoginRequest request) {

        TokenResponse tokenResponse = authService.login(request);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.AUTH_LOGIN_SUCCESS, tokenResponse));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 사용하여 새로운 Access Token을 발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<GlobalResponse> reissue(@RequestBody TokenRequest request) {
        TokenResponse response = authService.reissue(request.getRefreshToken());
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.AUTH_REISSUE_SUCCESS, response));
    }

    @Operation(summary = "로그아웃", description = "로그인된 사용자의 Refresh Token을 삭제하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse> logout(@AuthenticationPrincipal UserDetail userDetail) {
        authService.logout(userDetail.getUser().getId());
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.AUTH_LOGOUT_SUCCESS, null));
    }

}
