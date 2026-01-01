package com.club.site.debug;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/_debug")
public class DebugController {

    /**
     * 1️⃣ Firebase Admin 초기???�인
     */
    @GetMapping("/firebase")
    public Map<String, Object> firebaseStatus() {
        Map<String, Object> res = new HashMap<>();
        res.put("firebaseAppCount", FirebaseApp.getApps().size());
        res.put("timestamp", Instant.now().toString());
        return res;
    }

    /**
     * 2️⃣ Firestore write/read ?�스??
     */
    @PostMapping("/firestore/ping")
    public Map<String, Object> firestorePingWrite() throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        Map<String, Object> data = new HashMap<>();
        data.put("message", "pong");
        data.put("writtenAt", Instant.now().toString());

        db.collection("system")
                .document("ping")
                .set(data)
                .get();

        return data;
    }

    @GetMapping("/firestore/ping")
    public Map<String, Object> firestorePingRead() throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        DocumentSnapshot doc = db.collection("system")
                .document("ping")
                .get()
                .get();

        if (!doc.exists()) {
            throw new RuntimeException("ping document not found");
        }

        return doc.getData();
    }

    /**
     * 3️⃣ Firebase ID Token ?�증 ?�인
     */
    @GetMapping("/auth/whoami")
    public Map<String, Object> whoami(Authentication authentication) {
        Map<String, Object> res = new HashMap<>();

        if (authentication == null) {
            res.put("authenticated", false);
            return res;
        }

        res.put("authenticated", true);
        res.put("uid", authentication.getName());
        return res;
    }
}
