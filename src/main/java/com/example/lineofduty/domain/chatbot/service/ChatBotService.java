package com.example.lineofduty.domain.chatbot.service;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.ErrorMessage;
import com.example.lineofduty.common.model.enums.MessageType;
import com.example.lineofduty.common.model.response.PageResponse;
import com.example.lineofduty.domain.chatbot.ChatMessage;
import com.example.lineofduty.domain.chatbot.ChatRoom;
import com.example.lineofduty.domain.chatbot.dto.request.SendRequest;
import com.example.lineofduty.domain.chatbot.dto.response.ChatRoomResponse;
import com.example.lineofduty.domain.chatbot.dto.response.ThreadResponse;
import com.example.lineofduty.domain.chatbot.repository.ChatMessageRepository;
import com.example.lineofduty.domain.chatbot.repository.ChatRoomRepository;
import com.example.lineofduty.domain.user.User;
import com.example.lineofduty.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBotService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    // 내 채팅방 조회 (없으면 자동 생성)
    @Transactional
    public ChatRoomResponse getMyChatRoom(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorMessage.USER_NOT_FOUND));

        // 채팅방이 없으면 생성
        ChatRoom chatRoom = chatRoomRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.from(user);
                    return chatRoomRepository.save(newRoom);
                });

        Long messageCount = chatMessageRepository.countByChatRoomId(chatRoom.getId());
        Long threadCount = chatMessageRepository.countThreadsByRoomId(chatRoom.getId());

        return ChatRoomResponse.from(chatRoom, messageCount, threadCount);
    }

    // 채팅방 초기화 (메시지 전체 삭제)
    @Transactional
    public void resetChatRoom(Long userId) {

        // 유저의 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorMessage.CHATROOM_NOT_FOUND));

        // 해당 채팅방의 모든 메시지 조회
        List<ChatMessage> allMessages = chatMessageRepository.findByChatRoom(chatRoom);

        // 최상위 메시지(부모가 없는 메시지)만 필터링
        List<ChatMessage> topLevelMessages = allMessages.stream()
                .filter(ChatMessage::isTopLevel)
                .toList();

        // 부모 메시지들을 삭제 (JPA가 자식들을 먼저 지우는 과정을 수행함)
        chatMessageRepository.deleteAll(topLevelMessages);
    }

    // 메시지 전송 및 AI 응답 생성 (스레드 형식)
    @Transactional
    public ThreadResponse sendMessage(Long userId, SendRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorMessage.USER_NOT_FOUND));

        // 채팅방 조회 또는 생성
        ChatRoom chatRoom = chatRoomRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.from(user);
                    return chatRoomRepository.save(newRoom);
                });

        // USER 메시지 저장 (부모 메시지)
        ChatMessage userMessage = ChatMessage.from(chatRoom, user, request.getContent());
        ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);

        // AI 응답 생성
        long startTime = System.currentTimeMillis();
        String aiResponse = geminiService.generateResponse(request.getContent());
        long responseTime = System.currentTimeMillis() - startTime;

        // AI 메시지 저장 메타데이터
        Map<String, Object> metadata = geminiService.createMetadata(
                aiResponse.length() / 4,  // 대략적인 토큰 수
                responseTime
        );

        ChatMessage aiMessage = ChatMessage.from(chatRoom, savedUserMessage, aiResponse, metadata);
        ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);

        return ThreadResponse.from(savedUserMessage, savedAiMessage);
    }

    // 메시지 목록 조회 (스레드 형식)
    @Transactional(readOnly = true)
    public PageResponse<ThreadResponse> getMessages(Long userId, int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorMessage.CHATROOM_NOT_FOUND));

        // USER 메시지(스레드)만 조회
        Page<ChatMessage> threads = chatMessageRepository.findThreadsByRoomId(
                chatRoom.getId(),
                pageable
        );

        // 각 USER 메시지에 대한 AI 답글 조회
        Page<ThreadResponse> responses = threads.map(userMessage -> {
            ChatMessage aiReply = chatMessageRepository.findReplyByParentId(userMessage.getId())
                    .orElse(null);
            return ThreadResponse.from(userMessage, aiReply);
        });

        return PageResponse.from(responses);
    }

    // 특정 스레드 조회
    @Transactional(readOnly = true)
    public ThreadResponse getThread(Long userId, Long messageId) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorMessage.CHATROOM_NOT_FOUND));

        // 메시지 조회
        ChatMessage userMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorMessage.CHAT_MESSAGE_NOT_FOUND));

        // 권한 확인
        if (!userMessage.getChatRoom().getId().equals(chatRoom.getId())) {
            throw new CustomException(ErrorMessage.NO_CHECK_PERMISSION);
        }

        // USER 메시지가 아니면 에러
        if (userMessage.getMessageType() != MessageType.USER) {
            throw new CustomException(ErrorMessage.INVALID_REQUEST);
        }

        // AI 답글 조회
        ChatMessage aiReply = chatMessageRepository.findReplyByParentId(messageId)
                .orElse(null);

        return ThreadResponse.from(userMessage, aiReply);
    }
}