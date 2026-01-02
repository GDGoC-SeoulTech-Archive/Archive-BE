package com.club.site.common.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuditLogService {
    private final Firestore firestore;

    public AuditLogService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void write(String type, String actorUid, String targetUid, Map<String, Object> payload) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", type);
            data.put("actorUid", actorUid);
            data.put("targetUid", targetUid);
            data.put("payload", payload == null ? Map.of() : payload);
            data.put("createdAt", Timestamp.now());
            firestore.collection("audit_logs").add(data);
        } catch (Exception ignored) {
        }
    }
}


