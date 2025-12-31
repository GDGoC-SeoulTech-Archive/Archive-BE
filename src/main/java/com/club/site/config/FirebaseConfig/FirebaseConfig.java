package com.club.site.config.FirebaseConfig; // 패키지명 확인하세요

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // 이미 켜져 있으면 그거 씀
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        // ⚠️ 파일 경로가 틀리면 여기서 또 에러 납니다. 정확한지 확인!
        FileInputStream serviceAccount =
                new FileInputStream("src/main/resources/serviceAccountKey.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        return FirebaseApp.initializeApp(options);
    }

    // 👇 여기가 중요합니다! (수정된 부분)
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) { // 1. 괄호 안에 이걸 넣으세요
        // 2. 이렇게 하면 스프링이 "아! App이 먼저구나" 하고 App을 먼저 만듭니다.
        return FirebaseAuth.getInstance(firebaseApp);
    }
}