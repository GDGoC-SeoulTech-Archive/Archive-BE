package com.club.site.post.service;

import com.club.site.common.dto.PagedResult;
import com.club.site.common.service.AuditLogService;
import com.club.site.member.service.MemberService;
import com.club.site.post.dto.PostDto;
import com.club.site.util.CursorCodec;
import com.club.site.util.EventDate;
import com.club.site.util.FirestoreUtils;
import com.club.site.util.UrlUtils;
import com.club.site.web.ApiException;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PostService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final Firestore firestore;
    private final MemberService memberService;
    private final AuditLogService auditLogService;

    public PostService(Firestore firestore, MemberService memberService, AuditLogService auditLogService) {
        this.firestore = firestore;
        this.memberService = memberService;
        this.auditLogService = auditLogService;
    }

    public PagedResult<PostDto> listPosts(Integer pageSize, String cursor) throws Exception {
        int size = sanitizePageSize(pageSize);

        Query q = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId());

        CursorCodec.Cursor decoded = CursorCodec.decodeOrNull(cursor);
        if (decoded != null) {
            Timestamp ts = FirestoreUtils.fromMillis(decoded.tsMillis());
            if (ts != null) {
                q = q.startAfter(ts, decoded.id());
            }
        }

        List<QueryDocumentSnapshot> docs = q.limit(size + 1).get().get().getDocuments();
        boolean hasMore = docs.size() > size;
        List<? extends DocumentSnapshot> page = hasMore ? docs.subList(0, size) : docs;

        List<PostDto> items = new ArrayList<>();
        for (DocumentSnapshot doc : page) {
            items.add(toDto(doc));
        }

        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            DocumentSnapshot last = page.get(page.size() - 1);
            Timestamp ts = last.getTimestamp("createdAt");
            if (ts != null) {
                nextCursor = CursorCodec.encode(new CursorCodec.Cursor(ts.toDate().getTime(), last.getId()));
            }
        }
        return new PagedResult<>(items, nextCursor);
    }

    public PostDto getPost(String postId) throws Exception {
        DocumentSnapshot doc = firestore.collection("posts").document(postId).get().get();
        if (!doc.exists()) {
            throw new ApiException("NOT_FOUND", "Post not found", HttpStatus.NOT_FOUND);
        }
        return toDto(doc);
    }

    public PostDto createPost(String actorUid, String title, String body, String eventDate, List<String> imageUrls) throws Exception {
        String normalizedDate = EventDate.normalize(eventDate);
        List<String> images = sanitizeUrls(imageUrls);
        String thumbnailUrl = images.isEmpty() ? null : images.get(0);

        String authorName;
        try {
            authorName = memberService.getMemberByUid(actorUid).name();
        } catch (Exception e) {
            authorName = "Unknown";
        }

        Timestamp now = Timestamp.now();
        Map<String, Object> data = new HashMap<>();
        data.put("title", title.trim());
        data.put("body", body.trim());
        data.put("eventDate", normalizedDate);
        data.put("images", images);
        data.put("thumbnailUrl", thumbnailUrl);
        data.put("authorId", actorUid);
        data.put("authorName", authorName);
        data.put("createdAt", now);
        data.put("updatedAt", now);

        DocumentReference ref = firestore.collection("posts").document();
        ref.set(data).get();
        auditLogService.write("POST_CREATE", actorUid, ref.getId(), Map.of("title", title));
        return toDto(ref.get().get());
    }

    public PostDto updatePost(String actorUid, String postId, String title, String body, String eventDate, List<String> imageUrls) throws Exception {
        DocumentReference ref = firestore.collection("posts").document(postId);
        DocumentSnapshot existing = ref.get().get();
        if (!existing.exists()) {
            throw new ApiException("NOT_FOUND", "Post not found", HttpStatus.NOT_FOUND);
        }

        String normalizedDate = EventDate.normalize(eventDate);
        List<String> images = sanitizeUrls(imageUrls);
        String thumbnailUrl = images.isEmpty() ? null : images.get(0);

        Map<String, Object> update = new HashMap<>();
        update.put("title", title.trim());
        update.put("body", body.trim());
        update.put("eventDate", normalizedDate);
        update.put("images", images);
        update.put("thumbnailUrl", thumbnailUrl);
        update.put("updatedAt", Timestamp.now());

        ref.update(update).get();
        auditLogService.write("POST_UPDATE", actorUid, postId, Map.of("title", title));
        return toDto(ref.get().get());
    }

    public void deletePost(String actorUid, String postId) throws Exception {
        DocumentReference ref = firestore.collection("posts").document(postId);
        DocumentSnapshot existing = ref.get().get();
        if (!existing.exists()) {
            throw new ApiException("NOT_FOUND", "Post not found", HttpStatus.NOT_FOUND);
        }
        ref.delete().get();
        auditLogService.write("POST_DELETE", actorUid, postId, Map.of());
    }

    private static PostDto toDto(DocumentSnapshot doc) {
        return new PostDto(
                doc.getId(),
                doc.getString("title"),
                doc.getString("body"),
                doc.getString("eventDate"),
                FirestoreUtils.asStringList(doc.get("images")),
                doc.getString("thumbnailUrl"),
                doc.getString("authorId"),
                doc.getString("authorName"),
                FirestoreUtils.toIsoString(doc.getTimestamp("createdAt")),
                FirestoreUtils.toIsoString(doc.getTimestamp("updatedAt"))
        );
    }

    private static List<String> sanitizeUrls(List<String> urls) {
        if (urls == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String url : urls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            UrlUtils.requireHttpUrl(url, "imageUrls");
            out.add(url.trim());
        }
        return out;
    }

    private int sanitizePageSize(Integer pageSize) {
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}

