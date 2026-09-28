package com.example.lineofduty.domain.payment.service;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.ErrorMessage;
import com.example.lineofduty.domain.payment.dto.TossCancelRequest;
import com.example.lineofduty.domain.payment.dto.TossConfirmRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 토스페이먼츠 API 호출 전담. 응답 JSON을 그대로 돌려주고, 토스가 에러를 주면 응답에 "message" 필드가 들어있다.
 * (PaymentService에서 분리 - 테스트에서 실제 토스 대신 지연시간을 흉내 낸 가짜 응답으로 바꿔 끼우기 위함)
 */
@Component
public class TossPaymentClient {

    private static final String AUTHORIZATION = "Authorization";
    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_GET_BY_PAYMENTKEY_URL = "https://api.tosspayments.com/v1/payments";
    private static final String TOSS_GET_BY_ORDERID_URL = "https://api.tosspayments.com/v1/payments/orders";
    private static final String TOSS_CANCEL_URL_FORMAT = "https://api.tosspayments.com/v1/payments/%s/cancel";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${toss.secret.key}")
    private String secretKey;

    public JsonNode confirm(TossConfirmRequest request) {
        return send(HttpRequest.newBuilder()
                .uri(URI.create(TOSS_CONFIRM_URL))
                .header(AUTHORIZATION, encodeBasicSecretKey())
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(toJson(request)))
                .build());
    }

    public JsonNode getByPaymentKey(String paymentKey) {
        return send(HttpRequest.newBuilder()
                .uri(URI.create(TOSS_GET_BY_PAYMENTKEY_URL + paymentKey))
                .header(AUTHORIZATION, encodeBasicSecretKey())
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build());
    }

    public JsonNode getByOrderId(String orderNumber) {
        return send(HttpRequest.newBuilder()
                .uri(URI.create(TOSS_GET_BY_ORDERID_URL + orderNumber))
                .header(AUTHORIZATION, encodeBasicSecretKey())
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build());
    }

    public JsonNode cancel(String paymentKey, TossCancelRequest request) {
        return send(HttpRequest.newBuilder()
                .uri(URI.create(String.format(TOSS_CANCEL_URL_FORMAT, paymentKey)))
                .header(AUTHORIZATION, encodeBasicSecretKey())
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(toJson(request)))
                .build());
    }

    private String encodeBasicSecretKey() {
        return "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private String toJson(Object requestBody) {
        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorMessage.INVALID_REQUEST);
        }
    }

    private JsonNode send(HttpRequest httpRequest) {
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readTree(response.body());
        } catch (IOException ie) {   // 결제 조회 실패 시
            throw new CustomException(ErrorMessage.TOSS_PAYMENT_API_COMMUNICATION_FAILED);
        } catch (InterruptedException ie) {   // 결제 조회 실패 시
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorMessage.TOSS_API_INTERRUPTED);
        }
    }
}
