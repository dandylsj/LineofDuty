package com.example.lineofduty.domain.qna.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.qna.dto.request.QnaResisterRequest;
import com.example.lineofduty.domain.qna.dto.request.QnaUpdateRequest;
import com.example.lineofduty.domain.qna.dto.response.QnaInquiryListResponse;
import com.example.lineofduty.domain.qna.dto.response.QnaInquiryResponse;
import com.example.lineofduty.domain.qna.dto.response.QnaResisterResponse;
import com.example.lineofduty.domain.qna.dto.response.QnaUpdateResponse;
import com.example.lineofduty.domain.qna.service.QnaService;
import com.example.lineofduty.domain.user.dto.UserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Qna", description = "QnA 게시판 관련 API")
@RestController
@RequestMapping("/api/qnas")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @Operation(summary = "질문 등록", description = "사용자가 새로운 질문을 등록합니다.")
    @PostMapping("/{userId}")
    public ResponseEntity<GlobalResponse> qnaRegistrationApi(@AuthenticationPrincipal UserDetail userDetails, @RequestBody @Valid QnaResisterRequest request) {

        QnaResisterResponse response = qnaService.qnaRegistration(userDetails,request);

        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(SuccessMessage.QNA_CREATE_SUCCESS, response));
    }

    @Operation(summary = "질문 단건 조회", description = "특정 질문의 상세 내용을 조회합니다.")
    @GetMapping("/{qnaId}")
    public ResponseEntity<GlobalResponse> qnaInquiryApi(@PathVariable Long qnaId) {

        QnaInquiryResponse response = qnaService.qnaInquiryWithOptimisticLock(qnaId);

        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.success(SuccessMessage.QNA_READ_SUCCESS,response));

    }

    @Operation(summary = "질문 목록 조회", description = "질문 목록을 페이징 처리하여 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse> qnaInquiryListApi(@RequestParam(value = "page", defaultValue = "0") int page,
                                                            @RequestParam(value = "size", defaultValue = "10") int size,
                                                            @RequestParam(value = "sort", defaultValue = "id,desc") String sort,
                                                            @RequestParam(required = false) String keyword) {

        QnaInquiryListResponse response = qnaService.qnaInquiryListResponse(page, size, sort,keyword);

        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.success(SuccessMessage.QNA_LIST_READ_SUCCESS, response));
    }

    @Operation(summary = "질문 수정", description = "사용자가 등록한 질문 내용을 수정합니다.")
    @PutMapping("/{qnaId}")
    public ResponseEntity<GlobalResponse> qnaUpdateApi(@AuthenticationPrincipal UserDetail userDetails, @PathVariable Long qnaId,
                                                       @RequestBody @Valid QnaUpdateRequest request) {

        QnaUpdateResponse response = qnaService.qnaUpdate(userDetails,qnaId,request);

        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.success(SuccessMessage.QNA_UPDATE_SUCCESS, response));
    }

    @Operation(summary = "질문 삭제", description = "사용자가 등록한 질문을 삭제합니다.")
    @DeleteMapping("/{qnaId}")
    public ResponseEntity<GlobalResponse> qnaDeleteApi(@AuthenticationPrincipal UserDetail userDetails, @PathVariable Long qnaId) {

        qnaService.qnaDelete(userDetails,qnaId);

        return ResponseEntity.status(HttpStatus.OK).body(GlobalResponse.success(SuccessMessage.QNA_DELETE_SUCCESS, null));
    }

}
