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
import com.example.lineofduty.domain.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 결제 생성/승인/조회/취소.
 *
 * <p><b>토스 API 호출은 DB 트랜잭션 밖에서 한다.</b> 예전에는 승인/취소/조회 메서드 전체가 {@code @Transactional}이라
 * 토스 응답을 기다리는 수백 ms 동안 DB 커넥션을 붙잡고 있었고, 결제가 몰리면 커넥션 풀(기본 10개)이 바닥나서
 * 결제와 상관없는 다른 API까지 커넥션을 기다리며 느려졌다. 그래서 승인은 트랜잭션을 (1) 검증 + 재고 선점
 * (2) 토스 결과 반영 두 개로 짧게 나누고, 토스 호출은 그 사이에 트랜잭션 없이 한다.
 *
 * <p><b>재고는 토스 승인 "전에" 원자적 조건부 UPDATE로 선점한다.</b> 예전 방식(Redisson 분산 락 → 엔티티 차감)은
 * (a) 결제 트랜잭션에 합류해서 락이 커밋보다 먼저 풀렸고, (b) 가격 검증 때 이미 읽어둔 Product가 1차 캐시에 남아 있어서
 * 락을 잡은 뒤에도 옛날 재고 값으로 차감했다 - 동시 결제 시 초과 판매·재고 차감 누락이 났다. 또 재고 차감이 토스 승인
 * 뒤라서 재고가 부족하면 돈은 결제됐는데 주문은 롤백되는 경우가 생겼다.
 * {@code UPDATE ... SET stock = stock - ? WHERE id = ? AND stock >= ?}는 DB 행 잠금으로 원자적으로 처리되므로
 * 분산 락 없이도 정확하고, 재고가 없으면 토스를 부르기 전에 실패한다. 토스 승인이 실패하면 선점한 재고를 되돌린다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final TossPaymentClient tossPaymentClient;
    private final TransactionTemplate transactionTemplate;

    //토스 response key값 (토스 api 명세서 참고할 것)
    private static final String MESSAGE = "message";
    private static final String STATUS = "status";
    private static final String PAYMENT_KEY = "paymentKey";
    private static final String TOTAL_AMOUNT = "totalAmount";
    private static final String REQUESTED_AT = "requestedAt";
    private static final String APPROVED_AT = "approvedAt";
    private static final String ORDER_NAME = "orderName";
    private static final String ORDER_ID = "orderId";

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

    // 결제 승인 - 트랜잭션은 짧게 둘로 나누고, 토스 호출은 그 사이에 트랜잭션 없이 한다 (클래스 주석 참고)
    public PaymentConfirmResponse confirmPaymentService(PaymentConfirmRequest request) {
        String paymentKey = request.getPaymentKey();

        // 1) 검증 + 결제 진행 중 표시 + 재고 선점 (커밋)
        TossConfirmRequest tossRequest = transactionTemplate.execute(status -> reserveForConfirm(paymentKey));

        // 2) 토스로 결제 승인 요청 (트랜잭션 밖 - DB 커넥션을 잡고 있지 않음)
        JsonNode rootNode;
        try {
            rootNode = tossPaymentClient.confirm(tossRequest);
        } catch (RuntimeException e) {
            transactionTemplate.executeWithoutResult(status -> releaseReservation(paymentKey, PaymentStatus.READY));
            throw e;
        }

        // toss에서 에러를 출력할 시 선점한 재고를 돌려놓고 에러 반환
        if (rootNode.has(MESSAGE)) {
            transactionTemplate.executeWithoutResult(status -> releaseReservation(paymentKey, PaymentStatus.ABORTED));
            throw new CustomTossResponseException(rootNode.get(MESSAGE).asText());
        }

        // 3) 승인 결과 반영 (커밋)
        return transactionTemplate.execute(status -> completeConfirm(paymentKey, rootNode));
    }

    private TossConfirmRequest reserveForConfirm(String paymentKey) {

        // 승인할 결제(Payment) 찾아
        Payment payment = paymentRepository.findByPaymentKey(paymentKey).orElseThrow(
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
        List<OrderItem> orderItemList = payment.getOrder().getOrderItemList();
        long compareTotalPrice = 0;
        for (OrderItem item : orderItemList) {
            compareTotalPrice += item.getProduct().getPrice() * item.getQuantity();
        }

        if (compareTotalPrice != payment.getTotalPrice()) {
            throw new CustomException(ErrorMessage.INVALID_AMOUNT_PAYMENT);
        }

        // 같은 결제에 승인 요청이 동시에 두 번 들어오면(더블 클릭 등) 재고를 두 번 선점하지 않도록 한 요청만 통과시킨다
        if (paymentRepository.markInProgress(payment.getId()) == 0) {
            throw new CustomException(ErrorMessage.ALREADY_PROCESSED_PAYMENT);
        }

        // 주문 내역(List<orderItem>)에 맞추어서 재고(product) 선점 - 하나라도 부족하면 예외 → 이 트랜잭션 전체 롤백
        for (OrderItem orderItem : orderItemList) {
            if (productRepository.decreaseStockIfAvailable(orderItem.getProduct().getId(), orderItem.getQuantity()) == 0) {
                throw new CustomException(ErrorMessage.OUT_OF_STOCK);
            }
        }

        return new TossConfirmRequest(payment.getPaymentKey(), payment.getOrderNumber(), payment.getTotalPrice());
    }

    // 토스 승인이 실패하면 선점한 재고를 되돌리고 결제 상태를 되돌린다
    private void releaseReservation(String paymentKey, PaymentStatus rollbackStatus) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey).orElseThrow(
                () -> new CustomException(ErrorMessage.NOT_FOUND_PAYMENT)
        );
        for (OrderItem orderItem : payment.getOrder().getOrderItemList()) {
            productRepository.increaseStock(orderItem.getProduct().getId(), orderItem.getQuantity());
        }
        payment.updateStatus(rollbackStatus);
    }

    private PaymentConfirmResponse completeConfirm(String paymentKey, JsonNode rootNode) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey).orElseThrow(
                () -> new CustomException(ErrorMessage.NOT_FOUND_PAYMENT)
        );

        String status = rootNode.get(STATUS).asText();
        String tossPaymentKey = rootNode.get(PAYMENT_KEY).asText();
        long totalPrice = rootNode.get(TOTAL_AMOUNT).asLong();
        OffsetDateTime requestedAt = OffsetDateTime.parse(rootNode.get(REQUESTED_AT).asText());
        OffsetDateTime approvedAt = OffsetDateTime.parse(rootNode.get(APPROVED_AT).asText());

        // toss 반환 값에 맞추어 결제 정보 업데이트
        payment.updateByResponse(PaymentStatus.valueOf(status), tossPaymentKey, totalPrice, requestedAt, approvedAt);

        // 결제 끝난 주문서는 사용 종료 처리
        payment.getOrder().updateIsOrderCompleted(true);

        return PaymentConfirmResponse.from(payment);
    }

    // 결제 조회 (paymentKey) - 토스 조회는 트랜잭션 밖에서, 주문 내역 조회만 짧은 읽기 트랜잭션으로
    public PaymentGetResponse getPaymentByPaymentKeyService(String paymentKey) {

        // 토스로 결제 요청 보내
        JsonNode rootNode = tossPaymentClient.getByPaymentKey(paymentKey);

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

        List<OrderItemResponse> orderItemList = findOrderItems(orderNumber);

        return new PaymentGetResponse(paymentKey, orderName, orderNumber, orderItemList, PaymentStatus.valueOf(status), totalPrice, requestedAt, approvedAt);
    }

    // 결제 조회 (orderNumber) - 토스 조회는 트랜잭션 밖에서, 주문 내역 조회만 짧은 읽기 트랜잭션으로
    public PaymentGetResponse getPaymentByOrderIdService(String orderNumber) {

        // 토스로 결제 요청 보내
        JsonNode rootNode = tossPaymentClient.getByOrderId(orderNumber);

        // toss에서 에러를 출력할 시 에러 반환
        if (rootNode.has(MESSAGE)) {
            throw new CustomTossResponseException(rootNode.get(MESSAGE).asText());
        }

        // toss에서 정상 값을 반환할 시 값 추출
        String paymentKey = rootNode.get(PAYMENT_KEY).asText();
        String orderName = rootNode.get(ORDER_NAME).asText();

        List<OrderItemResponse> orderItemList = findOrderItems(orderNumber);

        String status = rootNode.get(STATUS).asText();
        long totalPrice = rootNode.get(TOTAL_AMOUNT).asLong();
        OffsetDateTime requestedAt = OffsetDateTime.parse(rootNode.get(REQUESTED_AT).asText());
        OffsetDateTime approvedAt = OffsetDateTime.parse(rootNode.get(APPROVED_AT).asText());

        // toss 반환 값에 맞추어 response 생성, 반환
        return new PaymentGetResponse(paymentKey, orderName, orderNumber, orderItemList, PaymentStatus.valueOf(status), totalPrice, requestedAt, approvedAt);
    }

    private List<OrderItemResponse> findOrderItems(String orderNumber) {
        TransactionTemplate readOnly = new TransactionTemplate(transactionTemplate.getTransactionManager());
        readOnly.setReadOnly(true);
        return readOnly.execute(status -> {
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new CustomException(ErrorMessage.ORDER_NOT_FOUND));
            return order.getOrderItemList().stream().map(OrderItemResponse::from).toList();
        });
    }

    // 결제 취소 - 검증과 결과 반영은 짧은 트랜잭션으로, 토스 취소 호출은 그 사이에 트랜잭션 없이 한다
    public PaymentCancelResponse cancelPaymentService(PaymentCancelRequest request, String paymentKey, long userId) {

        PaymentCancelResponse deletedBeforeApproval = transactionTemplate.execute(status -> {
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
            return null;
        });
        if (deletedBeforeApproval != null) {
            return deletedBeforeApproval;
        }

        // 토스로 결제 취소 요청 보내
        JsonNode rootNode = tossPaymentClient.cancel(paymentKey, new TossCancelRequest(request.getCancelReason()));

        // toss에서 에러를 출력할 시 에러 반환
        if (rootNode.has(MESSAGE)) {
            throw new CustomTossResponseException(rootNode.get(MESSAGE).asText());
        }

        return transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                    .orElseThrow(() -> new CustomException(ErrorMessage.NOT_FOUND_PAYMENT));

            String tossStatus = rootNode.get(STATUS).asText();
            long totalPrice = rootNode.get(TOTAL_AMOUNT).asLong();
            OffsetDateTime requestedAt = OffsetDateTime.parse(rootNode.get(REQUESTED_AT).asText());
            OffsetDateTime approvedAt = OffsetDateTime.parse(rootNode.get(APPROVED_AT).asText());

            // toss 반환 값에 맞추어 결제 정보 업데이트
            payment.updateByResponse(PaymentStatus.valueOf(tossStatus), paymentKey, totalPrice, requestedAt, approvedAt);

            // 각 주문 상품의 재고를 다시 증가시켜 (원자적 UPDATE)
            for (OrderItem orderItem : payment.getOrder().getOrderItemList()) {
                productRepository.increaseStock(orderItem.getProduct().getId(), orderItem.getQuantity());
            }

            return PaymentCancelResponse.from(payment);
        });
    }
}
