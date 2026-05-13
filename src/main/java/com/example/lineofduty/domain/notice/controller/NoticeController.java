package com.example.lineofduty.domain.notice.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.notice.dto.request.NoticeResisterRequest;
import com.example.lineofduty.domain.notice.dto.response.NoticeInquiryListResponse;
import com.example.lineofduty.domain.notice.dto.response.NoticeInquiryResponse;
import com.example.lineofduty.domain.notice.dto.response.NoticeResisterResponse;
import com.example.lineofduty.domain.notice.dto.response.NoticeUpdateResponse;
import com.example.lineofduty.domain.notice.service.NoticeService;
import com.example.lineofduty.domain.user.dto.UserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notice", description = "공지사항 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;


    @Operation(summary = "공지사항 등록", description = "관리자가 새로운 공지사항을 등록합니다.")
    @PostMapping("/admin/notices")
    public ResponseEntity<GlobalResponse> noticeResisterApi(
            @AuthenticationPrincipal UserDetail userDetails,
            @RequestBody NoticeResisterRequest request
    ) {
        NoticeResisterResponse response = noticeService.noticeResister(userDetails, request);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.NOTICE_CREATE_SUCCESS, response));
    }

    @Operation(summary = "공지사항 상세 조회", description = "특정 공지사항의 상세 내용을 조회합니다.")
    @GetMapping("/notices/{noticeId}")
    public ResponseEntity<GlobalResponse> noticeInquiryApi(@PathVariable Long noticeId) {

        NoticeInquiryResponse response = noticeService.noticeInquiry(noticeId);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.NOTICE_READ_SUCCESS, response));
    }

    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 페이징 처리하여 조회합니다.")
    @GetMapping("/notices")
    public ResponseEntity<GlobalResponse> noticeInquiryListApi(@RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "10") int size,
                                                               @RequestParam(value = "sort", defaultValue = "id,desc") String sort) {

        NoticeInquiryListResponse response = noticeService.noticeInquiryList(page, size, sort);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.NOTICE_READ_SUCCESS, response));
    }

    @Operation(summary = "공지사항 수정", description = "관리자가 기존 공지사항의 내용을 수정합니다.")
    @PutMapping("/admin/notices/{noticeId}")
    public ResponseEntity<GlobalResponse> noticeUpdateApi(@PathVariable Long noticeId, @AuthenticationPrincipal UserDetail userDetails,
                                                          @RequestBody NoticeResisterRequest request) {

        NoticeUpdateResponse response = noticeService.noticeUpdate(noticeId,userDetails,request);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.NOTICE_UPDATE_SUCCESS, response));
    }

    @Operation(summary = "공지사항 삭제", description = "관리자가 특정 공지사항을 삭제합니다.")
    @DeleteMapping("/admin/notices/{noticeId}")
    public ResponseEntity<GlobalResponse> noticeDelete(@PathVariable Long noticeId, @AuthenticationPrincipal UserDetail userDetails) {

        noticeService.noticeDelete(noticeId,userDetails);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.NOTICE_DELETE_SUCCESS, null));
    }
}
