package com.example.lineofduty.domain.qna.controller;


import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.qna.service.QnaAdminService;
import com.example.lineofduty.domain.qna.dto.request.QnaAdminAnswerRequest;
import com.example.lineofduty.domain.qna.dto.request.QnaAdminAnswerUpdateRequest;
import com.example.lineofduty.domain.qna.dto.response.QnaAdminAnswerResponse;
import com.example.lineofduty.domain.qna.dto.response.QnaAdminAnswerUpdateResponse;
import com.example.lineofduty.domain.user.dto.UserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/qna")
@RequiredArgsConstructor
public class QnaAdminController {

    private final QnaAdminService qnaAdminService;


    // 질문 관리자 답변 등록.
    @PostMapping("/{qnaId}")
    public ResponseEntity<GlobalResponse> qnaAdminAnswerApi(@PathVariable Long qnaId, @AuthenticationPrincipal UserDetail userDetails,
                                                            @RequestBody QnaAdminAnswerRequest request) {

        QnaAdminAnswerResponse response = qnaAdminService.qnaAdminAnswer(qnaId,userDetails,request);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.QNA_CREATE_SUCCESS, response));
    }

    // 질문 관리자 답변 수정
    @PutMapping("/{qnaId}")
    public ResponseEntity<GlobalResponse> qnaAdminAnswerUpdateApi(@PathVariable Long qnaId, @AuthenticationPrincipal UserDetail userDetails, @RequestBody QnaAdminAnswerUpdateRequest request) {

        QnaAdminAnswerUpdateResponse response = qnaAdminService.qnaAdminAnswerUpdate(qnaId,userDetails, request);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.QNA_UPDATE_SUCCESS, response));
    }



}
