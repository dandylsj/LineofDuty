package com.example.lineofduty.common.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessMessage {

    // 200 OK
    AUTH_SIGNUP_SUCCESS("회원가입 성공"),
    AUTH_LOGIN_SUCCESS("로그인 성공"),
    MY_READ_SUCCESS("내 정보 조회 성공"),
    USER_UPDATE_SUCCESS("내 정보 수정 성공"),
    USER_ALL_READ_SUCCESS("회원 전체 조회 성공"),
    USER_READ_SUCCESS("회원 정보 조회 성공"),
    AUTH_LOGOUT_SUCCESS("로그아웃 성공"),
    AUTH_REISSUE_SUCCESS("토큰 재발급 성공"),
    SEND_AUTHENTICATION_CODE("인증 코드 발송했습니다."),
    EMAIL_VERIFICATION_SUCCESSFUL("이메일 인증에 성공했습니다."),

    PRODUCT_GET_ONE_SUCCESS("상품 단건 조회 성공"),
    PRODUCT_GET_ALL_SUCCESS("상품 목록 조회 성공"),
    PRODUCT_UPDATE_SUCCESS("상품 수정 완료"),
    ORDER_GET_SUCCESS("주문이 성공적으로 조회되었습니다."),
    ORDER_UPDATE_SUCCESS("주문이 수정되었습니다."),

    PAYMENT_CONFIRM_SUCCESS("결제 승인 완료"),
    PAYMENT_GET_SUCCESS("결제 조회 완료"),
    PAYMENT_CANCEL_SUCCESS("결제 취소 완료"),

    QNA_CREATE_SUCCESS("질문이 생성되었습니다."),
    QNA_READ_SUCCESS("질문 단건 조회 성공"),
    QNA_UPDATE_SUCCESS("질문이 수정되었습니다"),
    QNA_LIST_READ_SUCCESS("질문 목록 조회 성공"),
    NOTICE_READ_SUCCESS("공지사항 상세 조회 성공"),
    NOTICE_UPDATE_SUCCESS("공지가 수정되었습니다."),
    LOG_READ_SUCCESS("로그 조회 성공"),
    FILE_UPLOAD_SUCCESS("파일 업로드 성공"),

    CHATROOM_READ_SUCCESS("채팅방 조회 성공"),
    CHATROOM_DELETE_SUCCESS("채팅방 초기화 성공"),
    CHAT_MESSAGE_CREATE_SUCCESS("채팅메세지 전송 성공"),


    // 201 CREATED
    NOTICE_CREATE_SUCCESS("공지사항이 등록되었습니다."),
    USER_CREATE_SUCCESS("회원가입이 완료되었습니다."),
    PRODUCT_CREATE_SUCCESS("상품 등록 완료"),
    ORDER_CREATE_SUCCESS("주문이 생성되었습니다."),
    PAYMENT_CREATE_SUCCESS("결제 요청 생성 완료"),

    // 204 NO Content
    USER_DELETE_SUCCESS("회원탈퇴 성공"),
    USER_DELETE_ADMIN_SUCCESS("관리자 회원 탈퇴 성공"),
    PRODUCT_DELETE_SUCCESS("상품 삭제 완료"),
    ORDER_DELETE_SUCCESS("주문이 취소되었습니다."),
    //200 enlistment success
    ENLISTMENT_SUCCESS("입영 가능 일정 조회 성공"),
    ENLISTMENT_APPLY_SUCCESS("입영 신청 완료"),
    ENLISTMENT_LIST_SUCCESS("입영 신청 조회 성공"),
    ENLISTMENT_CANCEL_SUCCESS("입영 신청이 취소 완료."),
    ENLISTMENT_APPROVE_SUCCESS("입영 신청 승인 완료."),
    DEFERMENTS_SUCCESS("연기 신청이 접수되었습니다."),
    DEFERMENTS_GET_SUCCESS("연기 신청 조회 성공"),
    QNA_DELETE_SUCCESS("질문이 삭제되었습니다"),
    DEFERMENTS_PROCEED("연기 신청이 처리 되었습니다."),
    NOTICE_DELETE_SUCCESS("공지가 삭제되었습니다."),
    SUMMARY_SUCCESS("요약 완료"),
    WEATHER_READ_SUCCESS("날씨정보 요청완료"),
    LOGIN_SUCCESS("로그인 성공"),

    BANNER_LIST_SUCCESS("배너 목록 조회 성공"),
    BANNER_CREATE_SUCCESS("배너 등록 완료"),
    BANNER_UPDATE_SUCCESS("배너 수정 완료"),
    BANNER_DELETE_SUCCESS("배너 삭제 완료"),
    CATEGORY_CREATE_SUCCESS("카테고리 생성 완료"),
    CATEGORY_READ_SUCCESS("카테고리 조회 성공"),
    CATEGORY_DELETE_SUCCESS("카테고리 삭제 완료");


    private final String message;


}



