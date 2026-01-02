package com.club.site.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(@Value("${FIREBASE_STORAGE_BUCKET:}") String storageBucket) throws IOException {
        ClassPathResource resource = new ClassPathResource("firebase-service.json");

        try (InputStream serviceAccount = resource.getInputStream()) {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount));

            if (storageBucket != null && !storageBucket.isBlank()) {
                builder.setStorageBucket(storageBucket.trim());
            }

            FirebaseOptions options = builder.build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        }
    }
}


