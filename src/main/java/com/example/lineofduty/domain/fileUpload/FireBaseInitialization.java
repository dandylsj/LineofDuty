package com.example.lineofduty.domain.fileUpload;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.ErrorMessage;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class FireBaseInitialization {

    @Value("${GOOGLE_CREDENTIALS}")
    private String googleCredentials;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                byte[] decodedBytes = Base64.getDecoder().decode(googleCredentials);
                ByteArrayInputStream serviceAccount = new ByteArrayInputStream(decodedBytes);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
            }
        } catch (Exception e) {
            throw new CustomException(ErrorMessage.INVALID_REQUEST);
        }
    }
}
