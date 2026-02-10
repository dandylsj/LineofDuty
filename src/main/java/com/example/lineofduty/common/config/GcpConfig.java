package com.example.lineofduty.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class GcpConfig {

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.client-email}")
    private String clientEmail;

    @Value("${gcp.private-key}")
    private String privateKey;

    @Value("${gcp.private-key-id}")
    private String privateKeyId;

    @Value("${gcp.client-id}")
    private String clientId;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        // privateKey의 줄바꿈 문자 처리
        // 환경 변수로 들어올 때 "\\n"으로 들어오는 경우와 "\n"으로 들어오는 경우를 모두 고려
        String fixedPrivateKey = privateKey.replace("\\n", "\n");
        
        // 혹시 모를 따옴표 제거 (필요한 경우에만)
        if (fixedPrivateKey.startsWith("\"") && fixedPrivateKey.endsWith("\"")) {
            fixedPrivateKey = fixedPrivateKey.substring(1, fixedPrivateKey.length() - 1);
        }

        // 2. JSON 구조를 Map으로 생성
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("type", "service_account");
        jsonMap.put("project_id", projectId);
        jsonMap.put("private_key_id", privateKeyId);
        jsonMap.put("private_key", fixedPrivateKey);
        jsonMap.put("client_email", clientEmail);
        jsonMap.put("client_id", clientId);
        jsonMap.put("auth_uri", "https://accounts.google.com/o/oauth2/auth");
        jsonMap.put("token_uri", "https://oauth2.googleapis.com/token");
        jsonMap.put("auth_provider_x509_cert_url", "https://www.googleapis.com/oauth2/v1/certs");
        jsonMap.put("universe_domain", "googleapis.com");

        ObjectMapper mapper = new ObjectMapper();
        String finalJson = mapper.writeValueAsString(jsonMap);
        
        // json을 스프링 내부에서 생성
        return GoogleCredentials.fromStream(
                new ByteArrayInputStream(finalJson.getBytes(StandardCharsets.UTF_8))
        );
    }
}
