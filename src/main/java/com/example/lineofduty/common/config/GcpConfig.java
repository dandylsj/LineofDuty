//package com.example.lineofduty.common.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.auth.oauth2.GoogleCredentials;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class GcpConfig {
//
//    @Value("${gcp.project-id}")
//    private String projectId;
//
//    @Value("${gcp.client-email}")
//    private String clientEmail;
//
//    @Value("${gcp.private-key}")
//    private String privateKey;
//
//    @Value("${gcp.private-key-id}")
//    private String privateKeyId;
//
//    @Value("${gcp.client-id}")
//    private String clientId;
//
//    @Bean
//    public GoogleCredentials googleCredentials() throws IOException {
//        String fixedPrivateKey = privateKey;
//
//        // 1. 따옴표 제거
//        if (fixedPrivateKey.startsWith("\"") && fixedPrivateKey.endsWith("\"")) {
//            fixedPrivateKey = fixedPrivateKey.substring(1, fixedPrivateKey.length() - 1);
//        }
//
//        // 2. Base64 인코딩 여부 확인 및 디코딩
//        // "-----BEGIN"으로 시작하지 않으면 Base64로 인코딩된 값으로 간주
//        if (!fixedPrivateKey.contains("-----BEGIN PRIVATE KEY-----")) {
//            try {
//                byte[] decodedBytes = Base64.getDecoder().decode(fixedPrivateKey);
//                fixedPrivateKey = new String(decodedBytes, StandardCharsets.UTF_8);
//            } catch (IllegalArgumentException e) {
//                // Base64 디코딩 실패 시, 기존 방식대로 처리 시도
//                // (로그를 남기면 좋겠지만 여기선 생략)
//            }
//        }
//
//        // 3. 줄바꿈 문자 처리 (Base64 디코딩 후에도 필요할 수 있음, 혹은 Plain Text일 경우)
//        fixedPrivateKey = fixedPrivateKey.replace("\\n", "\n");
//
//        // 2. JSON 구조를 Map으로 생성
//        Map<String, Object> jsonMap = new HashMap<>();
//        jsonMap.put("type", "service_account");
//        jsonMap.put("project_id", projectId);
//        jsonMap.put("private_key_id", privateKeyId);
//        jsonMap.put("private_key", fixedPrivateKey);
//        jsonMap.put("client_email", clientEmail);
//        jsonMap.put("client_id", clientId);
//        jsonMap.put("auth_uri", "https://accounts.google.com/o/oauth2/auth");
//        jsonMap.put("token_uri", "https://oauth2.googleapis.com/token");
//        jsonMap.put("auth_provider_x509_cert_url", "https://www.googleapis.com/oauth2/v1/certs");
//        jsonMap.put("universe_domain", "googleapis.com");
//
//        ObjectMapper mapper = new ObjectMapper();
//        String finalJson = mapper.writeValueAsString(jsonMap);
//
//        return GoogleCredentials.fromStream(
//                new ByteArrayInputStream(finalJson.getBytes(StandardCharsets.UTF_8))
//        );
//    }
//}
