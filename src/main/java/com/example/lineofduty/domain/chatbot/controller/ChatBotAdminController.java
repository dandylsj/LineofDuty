package com.example.lineofduty.domain.chatbot.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.common.model.response.PageResponse;
import com.example.lineofduty.domain.chatbot.dto.response.AdminListResponse;
import com.example.lineofduty.domain.chatbot.dto.response.ThreadResponse;
import com.example.lineofduty.domain.chatbot.service.ChatBotAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@Tag(name = "ChatBot Admin", description = "관리자용 챗봇 관리 API")
@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ChatBotAdminController {

    private final ChatBotAdminService chatBotAdminService;

    @Operation(summary = "챗봇 통계 조회", description = "관리자가 특정 기간 동안의 챗봇 이용 통계를 조회합니다.")
    @GetMapping("/statistics")
    public ResponseEntity<GlobalResponse> getChatStatistics(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> statistics = chatBotAdminService.getChatStatistics(startDate, endDate);
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(SuccessMessage.SUMMARY_SUCCESS, statistics));
    }

    @Operation(summary = "전체 채팅방 목록 조회", description = "관리자가 모든 사용자의 채팅방 목록을 페이징하여 조회합니다.")
    @GetMapping("/rooms")
    public ResponseEntity<GlobalResponse> getAllChatRooms(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sort, @RequestParam(defaultValue = "desc") String direction) {
        PageResponse<AdminListResponse> response = chatBotAdminService.getAllChatRooms(page, size, sort, direction);
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(SuccessMessage.CHATROOM_READ_SUCCESS, response));
    }

    @Operation(summary = "특정 유저의 대화 목록 조회", description = "관리자가 특정 사용자의 챗봇 대화(스레드) 내역을 페이징하여 조회합니다.")
    @GetMapping("/users/{userId}/messages")
    public ResponseEntity<GlobalResponse> getUserMessages(@PathVariable Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sort, @RequestParam(defaultValue = "desc") String direction) {
        PageResponse<ThreadResponse> response = chatBotAdminService.getUserMessages(userId, page, size, sort, direction);
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(SuccessMessage.CHATROOM_READ_SUCCESS, response));
    }
}
