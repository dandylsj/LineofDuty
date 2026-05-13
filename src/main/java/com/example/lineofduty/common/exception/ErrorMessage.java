package com.example.lineofduty.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorMessage {

    /* --- 400 Bad Request --- */
    // 공통/입력값 오류
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "필수 필드값이 누락되었습니다."),

    // 유저 관련
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_LENGTH(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다."),
    MISSING_EMAIL_OR_PASSWORD(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 누락되었습니다."),

    // 게시판(QnA/공지) 관련
    MISSING_QUESTION_CONTENT(HttpStatus.BAD_REQUEST, "질문 내용은 필수입니다."),
    MISSING_COMMENT_CONTENT(HttpStatus.BAD_REQUEST, "댓글 내용은 필수입니다."),
    PROFANITY_DETECTED(HttpStatus.BAD_REQUEST, "비속어가 포함되어 있습니다." ),
    NOT_BLANK(HttpStatus.BAD_REQUEST, "빈칸으로 작성할 수 없습니다." ),
    ANTI_PLAQUE_FUNCTION(HttpStatus.BAD_REQUEST, "1분에 2개까지 글작성이 가능합니다. 잠시후 다시 시도해주세요."),
    ALREADY_ANSWERED_CANNOT_MODIFY(HttpStatus.BAD_REQUEST, "관리자가 답변한 게시글은 수정할 수 없습니다."),

    // 주문/상품 관련
    MISSING_ORDER_ID(HttpStatus.BAD_REQUEST, "orderId가 누락되었습니다."),
    MISSING_PRODUCT_NAME_OR_DESCRIPTION(HttpStatus.BAD_REQUEST, "상품명과 상품설명은 필수입니다."),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, "가격은 1원 이상이어야 합니다."),
    INVALID_STOCK(HttpStatus.BAD_REQUEST, "재고는 1개 이상이어야 합니다."),

    // 동시성 제어 관련 에러 메시지 추가
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "다른 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),
    LOCK_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 오류가 발생했습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),

    // 결제 관련
    ALREADY_CANCELED_PAYMENT(HttpStatus.BAD_REQUEST, "이미 취소된 결제 입니다."),
    NOT_YET_CONFIRM(HttpStatus.BAD_REQUEST, "아직 승인되지않은 결제입니다."),
    ALREADY_PROCESSED_PAYMENT(HttpStatus.BAD_REQUEST, "이미 처리된 결제 입니다."),

    // 챗봇 관련
    AI_ONLY_COMMENT_USER_MESSAGE(HttpStatus.BAD_REQUEST, "AI 메시지는 USER 메시지에만 답글을 달 수 있습니다."),

    // 파일 업로드 관련
    FILE_IS_EMPTY(HttpStatus.BAD_REQUEST, "파일이 비어있습니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다. (jpg, jpeg, png, bmp만 가능)"),


    /* --- 401 Unauthorized --- */
    // 인증 실패 (로그인 필요)
    INVALID_AUTH_INFO(HttpStatus.UNAUTHORIZED, "잘못된 인증 정보입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    ACCESS_DENIED(HttpStatus.UNAUTHORIZED, "접근 권한이 없습니다."),
    ADMIN_PERMISSION_REQUIRED(HttpStatus.UNAUTHORIZED, "관리자 권한이 필요합니다."),
    INVALID_DEFERMENT_STATUS(HttpStatus.UNAUTHORIZED, "없는 연기 상태 입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),

    /* --- 403 Forbidden --- */
    // 인가 실패 (권한 부족 - 작성자가 아님 등)
    NO_MODIFY_PERMISSION(HttpStatus.FORBIDDEN, "수정 권한이 없습니다."),
    NO_DELETE_PERMISSION(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다."),
    USER_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다."),
    REJECT_PAYMENT(HttpStatus.FORBIDDEN, "결제 승인이 거절되었습니다."),
    INVALID_ADMIN_TOKEN(HttpStatus.BAD_REQUEST, "관리자 토큰값이 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "토큰값이 일치하지 않습니다."),
    USER_LOGOUT(HttpStatus.BAD_REQUEST, "로그아웃된 계정입니다."),
    NO_CHECK_PERMISSION(HttpStatus.INTERNAL_SERVER_ERROR, "권한이 없습니다."),


    /* --- 404 Not Found --- */
    // 리소스 없음
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "이메일을 찾을 수 없습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "입영 신청 내역이 없습니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문이 존재하지 않습니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "입영 일정이 존재하지 않습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지를 찾을 수 없습니다."),
    DEFERMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "연기 일정을 찾을 수 없습니다"),
    USER_DELETED_NOT_FOUND(HttpStatus.NOT_FOUND, "이미 탈퇴한 사용자입니다."),
    NOT_FOUND_PAYMENT(HttpStatus.NOT_FOUND, "존재하지 않는 결제 정보 입니다."),
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 메세지입니다."),

    /* --- 409 Conflict --- */
    // 데이터 충돌
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    DUPLICATE_RESIDENT_NUMBER(HttpStatus.CONFLICT, "이미 등록된 주민등록번호입니다."),
    ALREADY_PAID_ORDER(HttpStatus.CONFLICT, "이미 결제된 주문입니다."),
    DUPLICATE_SCHEDULE(HttpStatus.CONFLICT, "이미 신청된 유저입니다"),
    INVALID_APPLICATION_STATUS(HttpStatus.CONFLICT,"승인 할 수 없습니다"),
    NO_REMAINING_SLOTS(HttpStatus.CONFLICT,"입영 일정이 모두 소진 되었습니다"),
    INVALID_AMOUNT_PAYMENT(HttpStatus.CONFLICT, "결제할 금액 정보가 일치하지 않습니다."),
    SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "동시에 요청이 들어왔습니다."),
    ALREADY_CREATED_YEAR(HttpStatus.CONFLICT, "이미 해당년도 입영일정이 있습니다"),
    /* --- 500 Internal Server Error --- */
    // 외부 API 관련
    TOSS_API_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "toss 결제 요청 처리 중 문제가 발생했습니다."),
    TOSS_PAYMENT_API_COMMUNICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "toss 결제 시스템과 통신 중 오류가 발생했습니다."),
    WEATHER_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "기상청 API 호출 중 오류가 발생했습니다."),
    KAKAO_LOGIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 로그인 중 오류가 발생했습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "배너를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
