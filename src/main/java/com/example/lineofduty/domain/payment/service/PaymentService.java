package com.example.lineofduty.domain.payment.service;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.CustomTossResponseException;
import com.example.lineofduty.common.exception.ErrorMessage;
import com.example.lineofduty.domain.order.Order;
import com.example.lineofduty.domain.order.repository.OrderRepository;
import com.example.lineofduty.domain.orderItem.OrderItem;
import com.example.lineofduty.domain.orderItem.OrderItemResponse;
import com.example.lineofduty.domain.payment.Payment;
import com.example.lineofduty.domain.payment.PaymentStatus;
import com.example.lineofduty.domain.payment.dto.*;
import com.example.lineofduty.domain.payment.repository.PaymentRepository;
import com.example.lineofduty.domain.product.service.ProductFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductFacade productFacade;

    @Value("${toss.secret.key}")
    private String secretKey;

    //토스 response key값 (토스 api 명세서 참고할 것)
    private static final String AUTHORIZATION = "Authorization";
    private static final String MESSAGE = "message";
    private static final String STATUS = "status";
    private static final String PAYMENT_KEY = "paymentKey";
    private static final String TOTAL_AMOUNT = "totalAmount";
    private static final String REQUESTED_AT = "requestedAt";
    private static final String APPROVED_AT = "approvedAt";
    private static final String ORDER_NAME = "orderName";
    private static final String ORDER_ID = "orderId";

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_GET_BY_PAYMENTKEY_URL = "https://api.tosspayments.com/v1/payments";
    private static final String TOSS_GET_BY_ORDERID_URL = "https://api.tosspayments.com/v1/payments/orders";

    @Transactional
    public PaymentCreateResponse createPaymentService(PaymentCreateRequest request, Long userId) {

        // 결제할 주문서(order)를 찾아
        Order order = orderRepository.findById(request.getOrderId()).orElseThrow(
                () -> new CustomException(ErrorMessage.ORDER_NOT_FOUND)
        );

        // 이미 결제한 주문인지 확인해
        paymentRepository.findByOrder(order).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.DONE) {
                throw new CustomException(ErrorMessage.ALREADY_PAID_ORDER);
            }
            if (existing.getStatus() == PaymentStatus.CANCELED) {
                throw new CustomException(ErrorMessage.ALREADY_CANCELED_PAYMENT);
            }
            // READY / ABORTED 상태면 중간에 나간 것이므로 삭제 후 재생성 허용
            paymentRepository.delete(existing);
        });

        // 니가 이 결제에 접근 권한을 가지고 있는지 확인해
        if (!order.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorMessage.ACCESS_DENIED);
        }

        // 결제 기록(Payment) 남기기
        Payment payment = new Payment(order);

        String paymentKey = request.getPaymentKey();
        if (paymentKey != null) {
            payment.updatePaymentKey(paymentKey);
        }

        paymentRepository.save(payment);
        return PaymentCreateResponse.from(payment);
    }

    // 결제 승인
    @Transactional
    public PaymentConfirmResponse confirmPaymentService(PaymentConfirmRequest request) {

        // 승인할 결제(Payment) 찾아
        Payment payment = paymentRepository.findByPaymentKey(request.getPaymentKey()).orElseThrow(
                () -> new CustomException(ErrorMessage.NOT_FOUND_PAYMENT)
        );

        // 이미 승인된 결제일 경우
        if (payment.getStatus() == PaymentStatus.DONE) {
            throw new CustomException(ErrorMessage.ALREADY_PROCESSED_PAYMENT);
        }

        // 이미 취소된 결제일 경우
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            throw new CustomException(ErrorMessage.ALREADY_CANCELED_PAYMENT);
        }

        // 결제할 값에 조작이 가해졌는지 검사
        long compareTotalPrice = 0;
        for (OrderItem item : payment.getOrder().getOrderItemList()) {
            compareTotalPrice += item.getProduct().getPrice() * item.getQuantity();
        }

        if (compareTotalPrice != payment.getTotalPrice()) {
            throw new CustomException(ErrorMessage.INVALID_AMOUNT_PAYMENT);
        }

        // 토스로 결제 승인 요청 보내
        TossConfirmRequest requestBody = new TossConfirmRequest(payment.getPaymentKey(), payment.getOrderNumber(), payment.getTotalPrice());

        String body = createTossRequestBody(requestBody);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(TOSS_CONFIRM_URL))
                .header(AUTHORIZATION, encodeBasicSecretKey(secretKey))
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .build();

        JsonNode rootNode = extractTossResponse(httpRequest);

        // toss에서 에러를 출력할 시 에러 반환
        if (rootNode.has(MESSAGE)) {
            payment.updateStatus(PaymentStatus.ABORTED);
            throw new CustomTossResponseException(rootNode.get(MESSAGE).asText());
        }

        // 결제 승인 받으면 성공 처리해
        // 주문서(order)에서 주문 내역(List<orderItem>)을 가져와
        List<OrderItem> orderItemList = payment.getOrder().getOrderItemList();

            // 주문 내역(List<orderItem>)에 맞추어서 재고(product) 차감해
            for (OrderItem orderItem : orderItemList) {
                productFacade.decreaseStock(
                        orderItem.getProduct().getId(),
                        orderItem.getQuantity()
                );
            }

        String status = rootNode.get(STATUS).asText();
        String paymentKey = rootNode.get(PAYMENT_KEY).asText();
        long totalPrice = rootNode.get(TOTAL_AMOUNT).asLong();
        OffsetDateTime requestedAt = OffsetDateTime.parse(rootNode.get(REQUESTED_AT).asText());
        OffsetDateTime approvedAt = OffsetDateTime.parse(rootNode.get(APPROVED_AT).asText());

        // toss 반환 값에 맞추어 결제 정보 업데이트
        payment.updateByResponse(PaymentStatus.valueOf(status), paymentKey, totalPrice, requestedAt, approvedAt);

        // 결제 끝난 주문서는 사용 종료 처리
        payment.getOrder().updateIsOrderCompleted(true);

        return PaymentConfirmResponse.from(payment);
    }

    // 결제 조회 (paymentKey)
    @Transactional(readOnly = true)
    public PaymentGetResponse getPaymentByPaymentKeyService(String paymentKey) {

        // 토스로 결제 요청 보내

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(TOSS_GET_BY_PAYMENTKEY_URL + paymentKey))
                .header(AUTHORIZATION, encodeBasicSecretKey(secretKey))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();

        JsonNode rootNode = extractTossResponse(httpRequest);

        // toss에서 에러를 출력할 시 에러 반환
        if (rootNode.has(MESSAGE)) {
            throw new CustomTossResponseException(rootNode.get(MESSAGE).asText());
        }

        // toss에서 정상 값을 반환할 시 값 추출
        String orderName = rootNode.get(ORDER_NAME).asText();
        String orderNumber = rootNode.get(ORDER_ID).asText();
        long totalPrice = rootNode.get(TOTAL_AMOUNT).asLong();
        String status = rootNode.get(STATUS).asText();
        OffsetDateTime requestedAt = OffsetDateTime.parse(rootNode.get(REQUESTED_AT).asText());
        OffsetDateTime approvedAt = OffsetDateTime.parse(rootNode.get(APPROVED_AT).asText());

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(
                () -> new CustomException(ErrorMessage.ORDER_NOT_FOUND)
        );
        List<OrderItemResponse> orderItemList = order.getOrderItemList().stream().map(OrderItemResponse::from).toList();

        return new PaymentGetResponse(paymentKey, orderName, orderNumber, orderItemList, PaymentStatus.valueOf(status), totalPrice, requestedAt, approvedAt);
    }

    // 결제 조회 (orderNumber)
    @Transactional(readOnly = true)
    public PaymentGetResponse getPaymentByOrderIdService(String orderNumber) {

        // 토스로 결제 요청 보내

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(TOSS_GET_BY_ORDERID_URL + orderNumber))
                .header(AUTHORIZATION, encodeBasicSecretKey(secretKey))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();

        JsonNode rootNode = extractTossResponse(httpRequest);

        // toss에서 에러를 출력할 시 에러 반환
        if (rootNode.has(MESSAGE)) {
            throw new CustomTossResponseException(rootNode.get(MESSAGE).asText());
        }

        // toss에서 정상 값을 반환할 시 값 추출
        String paymentKey = rootNode.get(PAYMENT_KEY).asText();
        String orderName = rootNode.get(ORDER_NAME).asText();

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CustomException(ErrorMessage.ORDER_NOT_FOUND));
        List<OrderItemResponse> orderItemList = order.getOrderItemList().stream()
                .map(OrderItemResponse::from)
                .toList();

        String status = rootNode.get(STATUS).asText();
        long totalPrice = rootNode.get(TOTAL_AMOUNT).asLong();
        OffsetDateTime requestedAt = OffsetDateTime.parse(rootNode.get(REQUESTED_AT).asText());
        OffsetDateTime approvedAt = OffsetDateTime.parse(rootNode.get(APPROVED_AT).asText());

        // toss 반환 값에 맞추어 response 생성, 반환
        return new PaymentGetResponse(paymentKey, orderName, orderNumber, orderItemList, PaymentStatus.valueOf(status), totalPrice, requestedAt, approvedAt);
    }

    // 결제 취소
    @Transactional
    public PaymentCancelResponse cancelPaymentService(PaymentCancelRequest request, String paymentKey, long userId) {

        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new CustomException(ErrorMessage.NOT_FOUND_PAYMENT));

        // payment 삭제 권한 검사해
        Long paymentUserId = payment.getOrder().getUser().getId();
        if (!paymentUserId.equals(userId)) {
            throw new CustomException(ErrorMessage.ACCESS_DENIED);
        }

        // 아직 승인되지 않은 결제(READY)는 토스 승인 전이므로 DB에서 바로 삭제
        if (payment.getStatus() == PaymentStatus.READY) {
            paymentRepository.delete(payment);
            return PaymentCancelResponse.canceled(payment);
        }

        // 이미 취소, 환불된 결제일 경우
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            throw new CustomException(ErrorMessage.ALREADY_CANCELED_PAYMENT);
        }

        // 결제 취소 body 생성
        String tossCancelURL = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";

        TossCancelRequest requestBody = new TossCancelRequest(request.getCancelReason());
        String body = createTossRequestBody(requestBody);

        // 토스로 결제 취소 요청 보내
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(tossCancelURL))
                .header(AUTHORIZATION, encodeBasicSecretKey(secretKey))
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .build();

        JsonNode rootNode = extractTossResponse(httpRequest);

        // toss에서 에러를 출력할 시 에러 반환
        if (rootNode.has(MESSAGE)) {
            throw new CustomTossResponseException(rootNode.get(MESSAGE).asText());
        }

        String status = rootNode.get(STATUS).asText();
        long totalPrice = rootNode.get(TOTAL_AMOUNT).asLong();
        OffsetDateTime requestedAt = OffsetDateTime.parse(rootNode.get(REQUESTED_AT).asText());
        OffsetDateTime approvedAt = OffsetDateTime.parse(rootNode.get(APPROVED_AT).asText());

        // toss 반환 값에 맞추어 결제 정보 업데이트
        payment.updateByResponse(PaymentStatus.valueOf(status), paymentKey, totalPrice, requestedAt, approvedAt);

        // 주문서에서 주문 내역 가져오기
        List<OrderItem> orderItemList = payment.getOrder().getOrderItemList();

            // 각 주문 상품의 재고를 다시 증가시켜
            for (OrderItem orderItem : orderItemList) {
                productFacade.increaseStock(
                        orderItem.getProduct().getId(),
                        orderItem.getQuantity()
                );
            }

        return PaymentCancelResponse.from(payment);
    }

    private String encodeBasicSecretKey(String secretKey) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private String createTossRequestBody(Object requestBody) {

        ObjectMapper objectMapper = new ObjectMapper();

        String body;
        try {
            body = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorMessage.INVALID_REQUEST);
        }
        return body;
    }

    private JsonNode extractTossResponse(HttpRequest httpRequest) {
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());

            //response(json형식)를 java객체로 변환해 추출
            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readTree(response.body());
        } catch (IOException ie) {   // 결제 조회 실패 시
            throw new CustomException(ErrorMessage.TOSS_PAYMENT_API_COMMUNICATION_FAILED);
        } catch (InterruptedException ie) {   // 결제 조회 실패 시
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorMessage.TOSS_API_INTERRUPTED);
        }
    }

}