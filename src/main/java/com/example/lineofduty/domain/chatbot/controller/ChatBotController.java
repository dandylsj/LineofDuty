package com.example.lineofduty.domain.chatbot.controller;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.common.model.response.PageResponse;
import com.example.lineofduty.domain.chatbot.dto.request.SendRequest;
import com.example.lineofduty.domain.chatbot.dto.response.ChatRoomResponse;
import com.example.lineofduty.domain.chatbot.dto.response.ThreadResponse;
import com.example.lineofduty.domain.chatbot.service.ChatBotService;
import com.example.lineofduty.domain.user.dto.UserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ChatBot", description = "사용자 챗봇 관련 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    @Operation(summary = "내 채팅방 조회", description = "로그인한 사용자의 채팅방 정보를 조회합니다. 채팅방이 없으면 자동으로 생성됩니다.")
    @GetMapping("/room")
    public ResponseEntity<GlobalResponse> getMyChatRoom(@AuthenticationPrincipal UserDetail userDetail) {
        ChatRoomResponse response = chatBotService.getMyChatRoom(userDetail.getUser().getId());
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(SuccessMessage.CHATROOM_READ_SUCCESS, response));
    }

    @Operation(summary = "채팅방 초기화", description = "사용자의 채팅방 대화 내역을 모두 삭제하고 초기화합니다.")
    @DeleteMapping("/room/reset")
    public ResponseEntity<GlobalResponse> resetChatRoom(@AuthenticationPrincipal UserDetail userDetail) {
        chatBotService.resetChatRoom(userDetail.getUser().getId());
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.successNodata(SuccessMessage.CHATROOM_DELETE_SUCCESS));
    }

    @Operation(summary = "메시지 전송", description = "챗봇에게 메시지를 전송하고 질문과 답변이 포함된 스레드 형식의 응답을 받습니다.")
    @PostMapping("/messages")
    public ResponseEntity<GlobalResponse> sendMessage(@AuthenticationPrincipal UserDetail userDetail, @Valid @RequestBody SendRequest request) {
        ThreadResponse response = chatBotService.sendMessage(userDetail.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(SuccessMessage.CHAT_MESSAGE_CREATE_SUCCESS, response));
    }

    @Operation(summary = "메시지 목록 조회", description = "사용자의 과거 채팅 대화 스레드 목록을 페이징하여 조회합니다.")
    @GetMapping("/messages")
    public ResponseEntity<GlobalResponse> getMessages(@AuthenticationPrincipal UserDetail userDetail, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sort, @RequestParam(defaultValue = "desc") String direction) {
        PageResponse<ThreadResponse> response = chatBotService.getMessages(userDetail.getUser().getId(), page, size, sort, direction);
        return ResponseEntity.status(HttpStatus.OK).
                body(GlobalResponse.success(SuccessMessage.CHATROOM_READ_SUCCESS, response));
    }

    @Operation(summary = "특정 스레드 조회", description = "메시지 ID를 통해 특정 질문과 답변으로 구성된 스레드 상세 내역을 조회합니다.")
    @GetMapping("/messages/{messageId}/thread")
    public ResponseEntity<GlobalResponse> getThread(@AuthenticationPrincipal UserDetail userDetail, @PathVariable Long messageId) {
        ThreadResponse response = chatBotService.getThread(userDetail.getUser().getId(), messageId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success(SuccessMessage.CHATROOM_READ_SUCCESS, response));
    }
}
