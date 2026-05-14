package com.example.lineofduty.domain.chatbot.service;

import com.example.lineofduty.domain.notice.Notice;
import com.example.lineofduty.domain.notice.repository.NoticeRepository;
import com.example.lineofduty.domain.product.Product;
import com.example.lineofduty.domain.product.repository.ProductRepository;
import com.example.lineofduty.domain.qna.Qna;
import com.example.lineofduty.domain.qna.repository.QnaRepository;
import com.example.lineofduty.domain.enlistmentSchedule.EnlistmentSchedule;
import com.example.lineofduty.domain.enlistmentSchedule.repository.EnlistmentScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=%s";
    private static final String AI_MODEL = "gemini-1.5-flash-latest";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;
    private final NoticeRepository noticeRepository;
    private final QnaRepository qnaRepository;
    private final EnlistmentScheduleRepository enlistmentScheduleRepository;

    private static final String SYSTEM_PROMPT = """
            당신은 대한민국 병역 관련 전문 쇼핑몰 'Line of Duty'의 상담 AI입니다.
            사용자가 상품이나 공지사항 등에 대해 질문할 경우, 주어진 문맥(Context) 정보를 최우선으로 참고하여 정확하고 친절하게 답변해주세요.
            
            주요 답변 영역:
            1. 우리 사이트에서 판매 중인 상품 정보 (가격, 재고, 설명 등)
            2. 우리 사이트의 공지사항 및 안내
            3. 다른 사용자들이 질문한 QnA 정보 및 그에 대한 답변
            4. 현재 신청 가능한 입영 일정 정보 (날짜, 잔여 자리 등)
            5. 입영 연기, 신청 절차 등 일반적인 병역 관련 정보
            
            답변 시 다음을 지켜주세요:
            - 주어진 Context에 관련 정보가 있다면 반드시 그 정보를 기반으로 답변할 것
            - 관리자 정보, 비밀번호, 시스템 내부 로직 등은 절대 제공하지 말 것
            - Context에 정보가 없더라도, 병역과 관련된 일반적인 지식이 있다면 친절히 답변할 것
            - 관련 없는 질문에는 정중하게 "저는 병역 및 쇼핑몰 관련 안내를 돕는 챗봇입니다."라고 거절할 것
            """;

    public GeminiService(ProductRepository productRepository, NoticeRepository noticeRepository, 
                         QnaRepository qnaRepository, EnlistmentScheduleRepository enlistmentScheduleRepository) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        this.productRepository = productRepository;
        this.noticeRepository = noticeRepository;
        this.qnaRepository = qnaRepository;
        this.enlistmentScheduleRepository = enlistmentScheduleRepository;
    }

    public String generateResponse(String userMessage) {
        try {
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("${GEMINI_API_KEY}")) {
                log.warn("GEMINI_API_KEY is not set. Using fallback response.");
                return getFallbackResponse(userMessage);
            }

            // 1. 간단한 컨텍스트 주입 (DB 정보 조회)
            String context = buildContextData();
            
            // 2. 요청 바디 생성 (System Prompt + Context + User Message)
            String requestBody = createRequestBody(userMessage, context);
            String url = String.format(GEMINI_API_URL_TEMPLATE, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                log.error("Gemini API error: {}, {}", response.statusCode(), response.body());
                return getFallbackResponse(userMessage);
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            return getFallbackResponse(userMessage);
        }
    }
    
    // DB의 정보를 문자열로 요약하여 가져오는 메서드
    private String buildContextData() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- 아래는 우리 사이트의 현재 정보입니다 ---\n");
        
        try {
            sb.append("[현재 판매 중인 상품 목록]\n");
            List<Product> products = productRepository.findAll();
            for (Product p : products) {
                sb.append(String.format("- 상품명: %s, 가격: %d원, 재고: %d개, 설명: %s\n", 
                        p.getName(), p.getPrice(), p.getStock(), p.getDescription()));
            }
            
            sb.append("\n[최신 공지사항]\n");
            List<Notice> notices = noticeRepository.findAll();
            for (Notice n : notices) {
                sb.append(String.format("- 제목: %s, 내용: %s\n", n.getTitle(), n.getContent()));
            }
            
            sb.append("\n[자주 묻는 질문 (QnA)]\n");
            List<Qna> qnas = qnaRepository.findAll();
            for (Qna q : qnas) {
                sb.append(String.format("- 질문: %s, 내용: %s\n", q.getTitle(), q.getQuestionContent()));
                if (q.getAskContent() != null && !q.getAskContent().isEmpty()) {
                    sb.append(String.format("  -> 답변: %s\n", q.getAskContent()));
                }
            }
            
            sb.append("\n[현재 신청 가능한 입영 일정]\n");
            List<EnlistmentSchedule> schedules = enlistmentScheduleRepository.findAll();
            for (EnlistmentSchedule s : schedules) {
                sb.append(String.format("- 입영 날짜: %s, 잔여 자리: %d명\n", 
                        s.getEnlistmentDate(), s.getRemainingSlots()));
            }

        } catch (Exception e) {
            log.warn("Failed to fetch context data from DB", e);
        }
        
        sb.append("------------------------------------------\n");
        return sb.toString();
    }

    private String createRequestBody(String userMessage, String context) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // 프롬프트 결합: 시스템 프롬프트 + DB에서 가져온 컨텍스트 + 사용자 질문
        String combinedMessage = SYSTEM_PROMPT + "\n\n" + context + "\n\n사용자 질문: " + userMessage;

        Map<String, Object> part = Map.of("text", combinedMessage);
        Map<String, Object> content = Map.of("parts", List.of(part));
        
        requestBody.put("contents", List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 1000);
        requestBody.put("generationConfig", generationConfig);

        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create request body for Gemini", e);
        }
    }

    private String parseResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            return json.path("candidates").get(0)
                       .path("content")
                       .path("parts").get(0)
                       .path("text").asText("죄송합니다, 답변을 생성하는 데 문제가 발생했습니다.");
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            return "응답을 처리하는 중 오류가 발생했습니다.";
        }
    }

    private String getFallbackResponse(String userMessage) {
        return "현재 AI 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요. " +
               "만약 급한 문의라면 다음 키워드로 질문해보세요: " +
               "입영 일정, 연기 신청, 상품, 장바구니, 주문내역, 공지사항, QnA, 마이페이지";
    }

    public Map<String, Object> createMetadata(int tokens, long responseTime) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tokens", tokens);
        metadata.put("responseTime", responseTime);
        metadata.put("model", AI_MODEL);
        return metadata;
    }
}
