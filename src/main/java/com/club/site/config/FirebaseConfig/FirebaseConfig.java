package com.club.site.config.FirebaseConfig;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // 이미 초기화되어 있으면 그거 사용
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            // classpath에서 파일 찾기
            ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
            
            if (!resource.exists()) {
                log.warn("Firebase 설정 파일(serviceAccountKey.json)이 없습니다. Firebase 기능은 사용할 수 없습니다.");
                return null;
            }

            InputStream serviceAccount = resource.getInputStream();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK 초기화 완료");
            return app;
        } catch (IOException e) {
            log.error("Firebase 초기화 실패: {}", e.getMessage());
            log.warn("Firebase 기능 없이 서버를 계속 실행합니다.");
            return null;
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        if (firebaseApp == null) {
            log.warn("FirebaseApp이 초기화되지 않아 FirebaseAuth를 생성할 수 없습니다.");
            return null;
        }
        return FirebaseAuth.getInstance(firebaseApp);
    }
}