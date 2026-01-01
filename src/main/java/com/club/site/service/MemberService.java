package com.club.site.service;

import com.club.site.dto.PagedResult;
import com.club.site.dto.member.GithubDto;
import com.club.site.dto.member.MemberDto;
import com.club.site.dto.member.SocialLinkRequest;
import com.club.site.model.MemberStatus;
import com.club.site.model.Part;
import com.club.site.model.SocialLink;
import com.club.site.model.SocialLinkType;
import com.club.site.util.CursorCodec;
import com.club.site.util.FirestoreUtils;
import com.club.site.util.UrlUtils;
import com.club.site.web.ApiException;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MemberService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final Firestore firestore;
    private final SkillService skillService;

    public MemberService(Firestore firestore, SkillService skillService) {
        this.firestore = firestore;
        this.skillService = skillService;
    }

    public MemberDto bootstrap(
            String uid,
            String name,
            String generation,
            Part part,
            String photoUrl,
            String githubUsername
    ) throws Exception {
        DocumentReference ref = firestore.collection("members").document(uid);
        DocumentSnapshot existing = ref.get().get();

        Timestamp now = Timestamp.now();
        Map<String, Object> update = new HashMap<>();
        update.put("uid", uid);
        update.put("name", name.trim());
        update.put("generation", generation.trim());
        update.put("part", part.name());
        update.put("status", MemberStatus.ACTIVE.name());
        update.put("updatedAt", now);
        Map<String, Object> github = new HashMap<>();
        if (githubUsername != null) {
            github.put("username", githubUsername);
        }
        if (photoUrl != null) {
            github.put("photoUrl", photoUrl);
        }
        if (!github.isEmpty()) {
            update.put("github", github);
        }

        if (!existing.exists()) {
            update.put("createdAt", now);
            ref.set(update).get();
        } else {
            ref.set(update, SetOptions.merge()).get();
        }

        return toDto(ref.get().get());
    }

    public MemberDto getMe(String uid) throws Exception {
        return toDto(requireMember(uid));
    }

    public MemberDto updateMe(String uid, String bio, List<SocialLinkRequest> socialLinks, List<String> skillIds) throws Exception {
        DocumentSnapshot member = requireMember(uid);

        if (skillIds != null) {
            skillService.requireActiveSkills(skillIds);
        }

        List<String> beforeSkillIds = FirestoreUtils.asStringList(member.get("skillIds"));
        List<String> afterSkillIds = skillIds == null ? beforeSkillIds : distinctPreserveOrder(skillIds);

        Map<String, Object> update = new HashMap<>();
        if (bio != null) {
            update.put("bio", bio);
        }
        if (socialLinks != null) {
            List<Map<String, Object>> mapped = new ArrayList<>();
            for (SocialLinkRequest link : socialLinks) {
                UrlUtils.requireHttpUrl(link.url(), "socialLinks.url");
                SocialLinkType type;
                try {
                    type = SocialLinkType.valueOf(link.type().trim().toUpperCase(Locale.ROOT));
                } catch (Exception e) {
                    throw new ApiException("BAD_REQUEST", "Invalid socialLinks.type: " + link.type(), HttpStatus.BAD_REQUEST);
                }
                mapped.add(Map.of("type", type.name(), "url", link.url().trim()));
            }
            update.put("socialLinks", mapped);
        }
        if (skillIds != null) {
            update.put("skillIds", afterSkillIds);
        }
        update.put("updatedAt", Timestamp.now());

        WriteBatch batch = firestore.batch();
        DocumentReference ref = firestore.collection("members").document(uid);
        batch.set(ref, update, SetOptions.merge());

        Set<String> beforeSet = new HashSet<>(beforeSkillIds);
        Set<String> afterSet = new HashSet<>(afterSkillIds);

        Timestamp now = Timestamp.now();
        for (String add : afterSet) {
            if (!beforeSet.contains(add)) {
                DocumentReference idx = firestore.collection("skill_members").document(add).collection("members").document(uid);
                batch.set(idx, Map.of("uid", uid, "createdAt", now), SetOptions.merge());
            }
        }
        for (String remove : beforeSet) {
            if (!afterSet.contains(remove)) {
                DocumentReference idx = firestore.collection("skill_members").document(remove).collection("members").document(uid);
                batch.delete(idx);
            }
        }

        batch.commit().get();
        return toDto(ref.get().get());
    }

    public void anonymizeMe(String uid) throws Exception {
        DocumentSnapshot member = requireMember(uid);

        List<String> beforeSkillIds = FirestoreUtils.asStringList(member.get("skillIds"));
        Timestamp now = Timestamp.now();

        WriteBatch batch = firestore.batch();
        DocumentReference ref = firestore.collection("members").document(uid);

        Map<String, Object> update = new HashMap<>();
        update.put("status", MemberStatus.ANONYMIZED.name());
        update.put("name", "Unknown");
        update.put("bio", null);
        update.put("socialLinks", List.of());
        update.put("skillIds", List.of());
        update.put("updatedAt", now);
        batch.set(ref, update, SetOptions.merge());

        for (String skillId : beforeSkillIds) {
            DocumentReference idx = firestore.collection("skill_members").document(skillId).collection("members").document(uid);
            batch.delete(idx);
        }

        batch.commit().get();

        QuerySnapshot posts = firestore.collection("posts").whereEqualTo("authorId", uid).get().get();
        WriteBatch postBatch = firestore.batch();
        for (DocumentSnapshot doc : posts.getDocuments()) {
            postBatch.update(doc.getReference(), Map.of("authorName", "Unknown", "updatedAt", Timestamp.now()));
        }
        postBatch.commit().get();
    }

    public MemberDto getPublicMember(String uid) throws Exception {
        DocumentSnapshot member = requireMember(uid);
        return toDto(member, true);
    }

    public PagedResult<MemberDto> listMembers(
            String generation,
            String part,
            List<String> skillIds,
            Integer pageSize,
            String cursor
    ) throws Exception {
        int size = sanitizePageSize(pageSize);
        if (skillIds == null || skillIds.isEmpty()) {
            return listMembersSimple(generation, part, size, cursor);
        }
        return listMembersWithSkills(generation, part, distinctPreserveOrder(skillIds), size, cursor);
    }

    private PagedResult<MemberDto> listMembersSimple(String generation, String part, int pageSize, String cursor) throws Exception {
        Query q = firestore.collection("members");
        if (generation != null && !generation.isBlank()) {
            q = q.whereEqualTo("generation", generation.trim());
        }
        if (part != null && !part.isBlank()) {
            q = q.whereEqualTo("part", Part.parse(part).name());
        }
        q = q.orderBy("createdAt", Query.Direction.DESCENDING).orderBy(FieldPath.documentId());

        CursorCodec.Cursor decoded = CursorCodec.decodeOrNull(cursor);
        if (decoded != null) {
            Timestamp ts = FirestoreUtils.fromMillis(decoded.tsMillis());
            if (ts != null) {
                q = q.startAfter(ts, decoded.id());
            }
        }

        List<QueryDocumentSnapshot> docs = q.limit(pageSize + 1).get().get().getDocuments();

        boolean hasMore = docs.size() > pageSize;
        List<? extends DocumentSnapshot> page = hasMore ? docs.subList(0, pageSize) : docs;

        List<MemberDto> items = new ArrayList<>();
        for (DocumentSnapshot doc : page) {
            items.add(toDto(doc, true));
        }

        String nextCursor = null;
        if (hasMore) {
            DocumentSnapshot last = page.get(page.size() - 1);
            Timestamp ts = last.getTimestamp("createdAt");
            if (ts != null) {
                nextCursor = CursorCodec.encode(new CursorCodec.Cursor(ts.toDate().getTime(), last.getId()));
            }
        }
        return new PagedResult<>(items, nextCursor);
    }

    private PagedResult<MemberDto> listMembersWithSkills(
            String generation,
            String part,
            List<String> skillIds,
            int pageSize,
            String cursor
    ) throws Exception {
        List<Set<String>> sets = new ArrayList<>();
        for (String skillId : skillIds) {
            QuerySnapshot qs = firestore.collection("skill_members").document(skillId).collection("members").get().get();
            Set<String> uids = qs.getDocuments().stream().map(DocumentSnapshot::getId).collect(Collectors.toSet());
            sets.add(uids);
        }
        Set<String> intersection = intersectAll(sets);
        List<String> candidates = new ArrayList<>(intersection);

        List<DocumentSnapshot> memberDocs = new ArrayList<>();
        for (String uid : candidates) {
            DocumentSnapshot doc = firestore.collection("members").document(uid).get().get();
            if (!doc.exists()) {
                continue;
            }
            if (generation != null && !generation.isBlank() && !generation.trim().equals(doc.getString("generation"))) {
                continue;
            }
            if (part != null && !part.isBlank() && !Part.parse(part).name().equals(doc.getString("part"))) {
                continue;
            }
            memberDocs.add(doc);
        }

        memberDocs.sort((a, b) -> {
            Timestamp ta = a.getTimestamp("createdAt");
            Timestamp tb = b.getTimestamp("createdAt");
            int cmp;
            if (ta == null && tb == null) {
                cmp = 0;
            } else if (ta == null) {
                cmp = 1;
            } else if (tb == null) {
                cmp = -1;
            } else {
                cmp = tb.compareTo(ta);
            }
            if (cmp != 0) {
                return cmp;
            }
            return b.getId().compareTo(a.getId());
        });

        CursorCodec.Cursor decoded = CursorCodec.decodeOrNull(cursor);
        int startIdx = 0;
        if (decoded != null) {
            for (int i = 0; i < memberDocs.size(); i++) {
                DocumentSnapshot doc = memberDocs.get(i);
                Timestamp ts = doc.getTimestamp("createdAt");
                if (ts != null && ts.toDate().getTime() == decoded.tsMillis() && doc.getId().equals(decoded.id())) {
                    startIdx = i + 1;
                    break;
                }
            }
        }

        int endIdx = Math.min(startIdx + pageSize, memberDocs.size());
        List<DocumentSnapshot> page = memberDocs.subList(startIdx, endIdx);
        List<MemberDto> items = page.stream().map(d -> toDto(d, true)).toList();

        String nextCursor = null;
        if (endIdx < memberDocs.size() && !page.isEmpty()) {
            DocumentSnapshot last = page.get(page.size() - 1);
            Timestamp ts = last.getTimestamp("createdAt");
            if (ts != null) {
                nextCursor = CursorCodec.encode(new CursorCodec.Cursor(ts.toDate().getTime(), last.getId()));
            }
        }

        return new PagedResult<>(items, nextCursor);
    }

    private DocumentSnapshot requireMember(String uid) throws Exception {
        DocumentSnapshot doc = firestore.collection("members").document(uid).get().get();
        if (!doc.exists()) {
            throw new ApiException("NOT_FOUND", "Member not found", HttpStatus.NOT_FOUND);
        }
        return doc;
    }

    private static MemberDto toDto(DocumentSnapshot doc) {
        return toDto(doc, false);
    }

    private static MemberDto toDto(DocumentSnapshot doc, boolean applyAnonymization) {
        String status = doc.getString("status");
        boolean anonymized = MemberStatus.ANONYMIZED.name().equalsIgnoreCase(status);

        String name = doc.getString("name");
        String bio = doc.getString("bio");
        List<SocialLink> socialLinks = new ArrayList<>();
        for (Map<String, Object> link : FirestoreUtils.asMapList(doc.get("socialLinks"))) {
            String typeStr = String.valueOf(link.get("type"));
            String url = link.get("url") == null ? null : String.valueOf(link.get("url"));
            try {
                socialLinks.add(new SocialLink(SocialLinkType.valueOf(typeStr), url));
            } catch (Exception ignored) {
            }
        }

        List<String> skillIds = FirestoreUtils.asStringList(doc.get("skillIds"));

        Map<String, Object> githubMap = FirestoreUtils.asMap(doc.get("github"));
        GithubDto githubDto = githubMap == null ? null : new GithubDto(
                githubMap.get("username") == null ? null : String.valueOf(githubMap.get("username")),
                githubMap.get("photoUrl") == null ? null : String.valueOf(githubMap.get("photoUrl"))
        );

        if (applyAnonymization && anonymized) {
            name = "Unknown";
            bio = null;
            socialLinks = List.of();
            skillIds = List.of();
            githubDto = null;
        }

        return new MemberDto(
                doc.getId(),
                name,
                doc.getString("generation"),
                Part.parse(doc.getString("part")),
                bio,
                socialLinks,
                skillIds,
                githubDto,
                anonymized ? MemberStatus.ANONYMIZED : MemberStatus.ACTIVE,
                FirestoreUtils.toIsoString(doc.getTimestamp("createdAt")),
                FirestoreUtils.toIsoString(doc.getTimestamp("updatedAt"))
        );
    }

    private int sanitizePageSize(Integer pageSize) {
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static List<String> distinctPreserveOrder(List<String> values) {
        if (values == null) {
            return List.of();
        }
        LinkedHashSet<String> set = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(set);
    }

    private static Set<String> intersectAll(List<Set<String>> sets) {
        if (sets.isEmpty()) {
            return Set.of();
        }
        Set<String> out = new HashSet<>(sets.get(0));
        for (int i = 1; i < sets.size(); i++) {
            out.retainAll(sets.get(i));
        }
        return out;
    }
}

