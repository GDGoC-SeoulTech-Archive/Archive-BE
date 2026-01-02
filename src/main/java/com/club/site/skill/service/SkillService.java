package com.club.site.skill.service;

import com.club.site.common.service.AuditLogService;
import com.club.site.skill.dto.SkillDto;
import com.club.site.util.FirestoreUtils;
import com.club.site.util.NormalizationUtils;
import com.club.site.web.ApiException;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SkillService {
    private final Firestore firestore;
    private final AuditLogService auditLogService;

    public SkillService(Firestore firestore, AuditLogService auditLogService) {
        this.firestore = firestore;
        this.auditLogService = auditLogService;
    }

    public List<SkillDto> list(boolean activeOnly) throws Exception {
        Query q = firestore.collection("skills").orderBy("normalized");
        if (activeOnly) {
            q = q.whereEqualTo("active", true);
        }
        List<QueryDocumentSnapshot> docs = q.get().get().getDocuments();
        return docs.stream().map(SkillService::toDto).collect(Collectors.toList());
    }

    public SkillDto create(String displayName, String actorUid) throws Exception {
        String normalized = NormalizationUtils.normalizeSkillId(displayName);
        if (normalized == null || normalized.isBlank()) {
            throw new ApiException("BAD_REQUEST", "displayName is invalid", HttpStatus.BAD_REQUEST);
        }

        DocumentReference ref = firestore.collection("skills").document(normalized);
        DocumentSnapshot existing = ref.get().get();
        if (existing.exists()) {
            throw new ApiException("CONFLICT", "Skill already exists: " + normalized, HttpStatus.CONFLICT);
        }

        Timestamp now = Timestamp.now();
        Map<String, Object> data = new HashMap<>();
        data.put("displayName", displayName.trim());
        data.put("normalized", normalized);
        data.put("active", true);
        data.put("createdAt", now);
        data.put("updatedAt", now);
        ref.set(data).get();

        auditLogService.write("SKILL_CREATE", actorUid, normalized, Map.of("displayName", displayName));
        return toDto(ref.get().get());
    }

    public SkillDto patchActive(String skillId, boolean active, String actorUid) throws Exception {
        DocumentReference ref = firestore.collection("skills").document(skillId);
        DocumentSnapshot existing = ref.get().get();
        if (!existing.exists()) {
            throw new ApiException("NOT_FOUND", "Skill not found", HttpStatus.NOT_FOUND);
        }

        ref.update(Map.of("active", active, "updatedAt", Timestamp.now())).get();
        auditLogService.write("SKILL_PATCH", actorUid, skillId, Map.of("active", active));
        return toDto(ref.get().get());
    }

    public void requireActiveSkills(List<String> skillIds) throws Exception {
        if (skillIds == null || skillIds.isEmpty()) {
            return;
        }
        for (String skillId : skillIds) {
            DocumentSnapshot doc = firestore.collection("skills").document(skillId).get().get();
            if (!doc.exists()) {
                throw new ApiException("BAD_REQUEST", "Unknown skillId: " + skillId, HttpStatus.BAD_REQUEST);
            }
            Boolean active = doc.getBoolean("active");
            if (active == null || !active) {
                throw new ApiException("BAD_REQUEST", "Inactive skillId: " + skillId, HttpStatus.BAD_REQUEST);
            }
        }
    }

    private static SkillDto toDto(DocumentSnapshot doc) {
        return new SkillDto(
                doc.getId(),
                doc.getString("displayName"),
                doc.getString("normalized"),
                Boolean.TRUE.equals(doc.getBoolean("active")),
                FirestoreUtils.toIsoString(doc.getTimestamp("createdAt")),
                FirestoreUtils.toIsoString(doc.getTimestamp("updatedAt"))
        );
    }
}

