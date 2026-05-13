package com.example.lineofduty.domain.qna.controller;


import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.qna.service.QnaAdminService;
import com.example.lineofduty.domain.qna.dto.request.QnaAdminAnswerRequest;
import com.example.lineofduty.domain.qna.dto.request.QnaAdminAnswerUpdateRequest;
import com.example.lineofduty.domain.qna.dto.response.QnaAdminAnswerResponse;
import com.example.lineofduty.domain.qna.dto.response.QnaAdminAnswerUpdateResponse;
import com.example.lineofduty.domain.user.dto.UserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Qna-Admin", description = "QnA 관리자용 API")
@RestController
@RequestMapping("/api/admin/qna")
@RequiredArgsConstructor
public class QnaAdminController {

    private final QnaAdminService qnaAdminService;


    @Operation(summary = "관리자 답변 등록", description = "관리자가 사용자의 질문에 대한 답변을 등록합니다.")
    @PostMapping("/{qnaId}")
    public ResponseEntity<GlobalResponse> qnaAdminAnswerApi(@PathVariable Long qnaId, @AuthenticationPrincipal UserDetail userDetails,
                                                            @RequestBody QnaAdminAnswerRequest request) {

        QnaAdminAnswerResponse response = qnaAdminService.qnaAdminAnswer(qnaId,userDetails,request);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.QNA_CREATE_SUCCESS, response));
    }

    @Operation(summary = "관리자 답변 수정", description = "관리자가 기존에 등록한 답변 내용을 수정합니다.")
    @PutMapping("/{qnaId}")
    public ResponseEntity<GlobalResponse> qnaAdminAnswerUpdateApi(@PathVariable Long qnaId, @AuthenticationPrincipal UserDetail userDetails, @RequestBody QnaAdminAnswerUpdateRequest request) {

        QnaAdminAnswerUpdateResponse response = qnaAdminService.qnaAdminAnswerUpdate(qnaId,userDetails, request);

        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.QNA_UPDATE_SUCCESS, response));
    }

}
