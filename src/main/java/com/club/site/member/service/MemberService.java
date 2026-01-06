package com.club.site.member.service;

import com.club.site.common.exception.BusinessException;
import com.club.site.common.error.ErrorCode;
import com.club.site.member.dto.*;
import com.club.site.model.MemberStatus;
import com.club.site.model.Part;
import com.club.site.model.SocialLink;
import com.club.site.model.SocialLinkType;
import com.club.site.util.FirestoreUtils;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MemberService {

    @Value("${pagination.default.page-size:20}")
    private int defaultPageSize;

    @Value("${pagination.max.page-size:50}")
    private int maxPageSize;

    // 허용된 part 값들
    private static final Set<String> VALID_PARTS = Set.of("WEB-FE", "WEB-BE", "App", "AI", "Design");

    // 컨트롤러가 호출하는 그 메서드!
    public String saveMockData() {
        Firestore db = FirestoreClient.getFirestore();
        List<MemberDTO> mockList = createMockMembers();

        int count = 0;
        int indexCount = 0;
        for (MemberDTO member : mockList) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("uid", member.uid());
                data.put("name", member.name());
                data.put("generation", member.generation());
                data.put("part", member.part().name()); // enum name으로 저장 ("APP", "WEB_FE" 등)
                data.put("bio", member.bio());
                data.put("socialLinks", member.socialLinks());
                data.put("skillIds", member.skillIds());
                data.put("github", member.github());
                data.put("status", member.status().name());
                if (member.createdAt() != null) {
                    data.put("createdAt", Timestamp.parseTimestamp(member.createdAt()));
                }
                if (member.updatedAt() != null) {
                    data.put("updatedAt", Timestamp.parseTimestamp(member.updatedAt()));
                }
                
                db.collection("members").document(member.uid()).set(data);
                count++;

                // 2. 역인덱스 생성 (skillIds가 있는 경우)
                if (member.skillIds() != null && !member.skillIds().isEmpty()) {
                    for (String skillId : member.skillIds()) {
                        try {
                            Map<String, Object> indexData = new HashMap<>();
                            indexData.put("uid", member.uid());
                            indexData.put("createdAt", Timestamp.now());

                            db.collection("skill_members")
                                    .document(skillId)
                                    .collection("members")
                                    .document(member.uid())
                                    .set(indexData);
                            indexCount++;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count + "명의 멤버 저장 완료! (역인덱스 " + indexCount + "개 생성)";
    }

    private List<MemberDTO> createMockMembers() {
        List<MemberDTO> list = new ArrayList<>();
        Timestamp now = Timestamp.now();
        String nowStr = FirestoreUtils.toIsoString(now);


        // 가연
        list.add(new MemberDTO(
                "2",
                "이가연",
                "5기",
                Part.parse("WEB-FE"),
                "프론트엔드 깎는 장인",
                null,
                List.of("React", "Vue", "Tailwind"),
                null,
                MemberStatus.ACTIVE,
                nowStr,
                nowStr
        ));

        // 대훈
        list.add(new MemberDTO(
                "3",
                "권대훈",
                "5기",
                Part.parse("AI"),
                "AI가 세상을 지배한다",
                null,
                List.of("Python", "TensorFlow", "React"), // React 공통 스킬 추가 (AND 필터 테스트용)
                null,
                MemberStatus.ACTIVE,
                nowStr,
                nowStr
        ));

        // 민석
        list.add(new MemberDTO(
                "4",
                "최민석",
                "5기",
                Part.parse("Design"),
                "하이하이~~",
                null,
                List.of("Figma", "React"), // React 공통 스킬 추가 (AND 필터 테스트용)
                null,
                MemberStatus.ACTIVE,
                nowStr,
                nowStr
        ));

        // 채영
        list.add(new MemberDTO(
                "5",
                "임채영",
                "5기",
                Part.parse("WEB-BE"),
                "서버 짓는 여인",
                null,
                List.of("Spring Boot", "Java", "React"), // React 공통 스킬 추가 (AND 필터 테스트용)
                null,
                MemberStatus.ACTIVE,
                nowStr,
                nowStr
        ));

        return list;
    }

    /**
     * 멤버 리스트 조회 (필터 + 페이지네이션)
     */
    public MemberListResponse getMembers(
            String generation,
            String part,
            List<String> skillIds,
            String q,
            Integer pageSize,
            String cursor
    ) {
        // 1. pageSize 검증
        int validPageSize = validatePageSize(pageSize);

        // 2. part 검증
        if (part != null && !VALID_PARTS.contains(part)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "유효하지 않은 part 값입니다: " + part);
        }

        try {
            Firestore db = FirestoreClient.getFirestore();
            List<MemberDTO> members;

            // 3. skillIds가 있으면 역인덱스 사용
            if (skillIds != null && !skillIds.isEmpty()) {
                members = getMembersBySkillIds(db, skillIds, generation, part, q);
            } else {
                // 4. skillIds가 없으면 일반 쿼리
                members = getMembersByQuery(db, generation, part, q);
            }

            // 5. 정렬 (createdAt desc)
            members.sort((a, b) -> {
                if (a.createdAt() == null && b.createdAt() == null) return 0;
                if (a.createdAt() == null) return 1;
                if (b.createdAt() == null) return -1;
                return b.createdAt().compareTo(a.createdAt()); // desc
            });

            // 6. cursor 파싱 및 필터링
            if (cursor != null && !cursor.isEmpty()) {
                members = applyCursor(members, cursor);
            }

            // 7. 페이지네이션 적용
            List<MemberDTO> pagedMembers = members.stream()
                    .limit(validPageSize + 1) // nextCursor 확인을 위해 +1
                    .collect(Collectors.toList());

            // 8. nextCursor 생성
            String nextCursor = null;
            if (pagedMembers.size() > validPageSize) {
                MemberDTO lastMember = pagedMembers.get(validPageSize - 1);
                nextCursor = createCursor(lastMember);
                pagedMembers = pagedMembers.subList(0, validPageSize);
            }

            // 9. MemberDTO를 MemberListItemDTO로 변환
            List<MemberListItemDTO> listItems = pagedMembers.stream()
                    .map(this::convertToListItem)
                    .collect(Collectors.toList());

            return new MemberListResponse(listItems, nextCursor);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore 조회 실패: " + e.getMessage());
        }
    }

    /**
     * skillIds가 있을 때 역인덱스 사용
     */
    private List<MemberDTO> getMembersBySkillIds(
            Firestore db,
            List<String> skillIds,
            String generation,
            String part,
            String q
    ) throws InterruptedException, ExecutionException {
        log.info("역인덱스 조회 시작 - skillIds: {}", skillIds);
        
        // 1. 각 skillId에 대해 skill_members/{skillId}/members에서 uid 목록 조회
        List<Set<String>> uidSets = new ArrayList<>();
        for (String skillId : skillIds) {
            try {
                ApiFuture<QuerySnapshot> future = db.collection("skill_members")
                        .document(skillId)
                        .collection("members")
                        .get();
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                Set<String> uids = documents.stream()
                        .map(QueryDocumentSnapshot::getId)
                        .collect(Collectors.toSet());
                log.info("skillId '{}'에 대한 uid 개수: {}", skillId, uids.size());
                uidSets.add(uids);
            } catch (Exception e) {
                // skillId가 존재하지 않으면 빈 Set 추가
                log.warn("skillId '{}' 조회 실패: {}", skillId, e.getMessage());
                uidSets.add(new HashSet<>());
            }
        }

        // 2. 교집합 계산
        if (uidSets.isEmpty()) {
            log.warn("uidSets가 비어있음");
            return new ArrayList<>();
        }

        Set<String> intersection = new HashSet<>(uidSets.get(0));
        for (int i = 1; i < uidSets.size(); i++) {
            intersection.retainAll(uidSets.get(i));
        }

        log.info("교집합 uid 개수: {}", intersection.size());

        // 교집합이 비어있으면 빈 리스트 반환
        if (intersection.isEmpty()) {
            log.warn("교집합이 비어있음 - skillIds: {}", skillIds);
            return new ArrayList<>();
        }

        // 3. 교집합 uid들을 members/{uid}에서 배치 조회
        List<MemberDTO> members = new ArrayList<>();
        for (String uid : intersection) {
            try {
                ApiFuture<com.google.cloud.firestore.DocumentSnapshot> future = db.collection("members")
                        .document(uid)
                        .get();
                com.google.cloud.firestore.DocumentSnapshot document = future.get();
                if (document.exists()) {
                    try {
                        MemberDTO member = convertDocumentToMemberDTO(document);
                        if (member != null) {
                            // 4. generation/part 필터 적용
                            // part는 enum name으로 저장되어 있으므로 비교
                            boolean partMatches = true;
                            if (part != null && !part.isEmpty()) {
                                try {
                                    Part partEnum = Part.parse(part);
                                    partMatches = member.part() != null && member.part().name().equals(partEnum.name());
                                } catch (Exception e) {
                                    partMatches = false;
                                }
                            }
                            // 4-1. generation/part 필터 적용
                            boolean generationMatches = generation == null || generation.equals(member.generation());
                            // 4-2. 이름 검색 필터 적용 (q 파라미터)
                            boolean nameMatches = q == null || q.isEmpty() || 
                                    (member.name() != null && member.name().toLowerCase().contains(q.toLowerCase()));
                            
                            if (generationMatches && partMatches && nameMatches) {
                                members.add(member);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("문서 변환 실패 - uid: {}, error: {}", uid, e.getMessage());
                    }
                }
            } catch (Exception e) {
                // 개별 문서 조회 실패는 무시
                e.printStackTrace();
            }
        }

        return members;
    }

    /**
     * skillIds가 없을 때 일반 쿼리
     */
    private List<MemberDTO> getMembersByQuery(
            Firestore db,
            String generation,
            String part,
            String q
    ) throws InterruptedException, ExecutionException {
        Query query = db.collection("members");

        // generation 필터
        if (generation != null && !generation.isEmpty()) {
            query = query.whereEqualTo("generation", generation);
        }

        // part 필터 - enum name으로 변환 (Firestore에 enum name으로 저장되므로)
        if (part != null && !part.isEmpty()) {
            try {
                Part partEnum = Part.parse(part);
                query = query.whereEqualTo("part", partEnum.name()); // enum name으로 쿼리
            } catch (Exception e) {
                log.warn("Part 변환 실패 - part: {}, error: {}", part, e.getMessage());
                // 변환 실패 시 빈 결과 반환
                return new ArrayList<>();
            }
        }

        // 쿼리 실행
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<MemberDTO> members = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            try {
                MemberDTO member = convertDocumentToMemberDTO(document);
                if (member != null) {
                    // 이름 검색 필터 적용 (q 파라미터)
                    // Firestore는 부분 문자열 검색이 제한적이므로 서버에서 필터링
                    if (q == null || q.isEmpty() || 
                            (member.name() != null && member.name().toLowerCase().contains(q.toLowerCase()))) {
                        members.add(member);
                    }
                }
            } catch (Exception e) {
                log.warn("문서 변환 실패 - uid: {}, error: {}", document.getId(), e.getMessage());
            }
        }

        return members;
    }

    /**
     * cursor 적용 (cursor 이후의 데이터만 반환)
     * cursor 형식: "timestampSeconds:timestampNanos:uid"
     */
    private List<MemberDTO> applyCursor(List<MemberDTO> members, String cursor) {
        try {
            String[] parts = cursor.split(":");
            if (parts.length < 3) {
                return members; // 잘못된 cursor는 무시
            }

            long seconds = Long.parseLong(parts[0]);
            int nanos = Integer.parseInt(parts[1]);
            String cursorUid = parts[2];
            Timestamp cursorTimestamp = Timestamp.ofTimeSecondsAndNanos(seconds, nanos);

            // cursor 이후의 데이터만 필터링 (createdAt desc 기준이므로 더 작은 값이 다음 페이지)
            return members.stream()
                    .filter(member -> {
                        if (member.createdAt() == null) return false;
                        // createdAt이 String이므로 Timestamp로 변환 필요
                        try {
                            // ISO 문자열을 Timestamp로 변환
                            if (member.createdAt() == null) return false;
                            java.time.Instant instant = java.time.Instant.parse(member.createdAt());
                            Timestamp memberTs = Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
                            int compare = memberTs.compareTo(cursorTimestamp);
                            if (compare > 0) return true; // createdAt이 cursor보다 이후 (더 최신)
                            if (compare < 0) return false; // createdAt이 cursor보다 이전
                            // createdAt이 같으면 uid로 비교 (desc 정렬이므로 uid가 더 작은 것이 다음)
                            return member.uid() != null && member.uid().compareTo(cursorUid) < 0;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // cursor 파싱 실패 시 전체 반환
            return members;
        }
    }

    /**
     * cursor 생성 (seconds:nanos:uid 형식)
     */
    private String createCursor(MemberDTO member) {
        if (member.createdAt() == null || member.uid() == null) {
            return null;
        }
        try {
            // ISO 문자열을 Timestamp로 변환
            java.time.Instant instant = java.time.Instant.parse(member.createdAt());
            Timestamp timestamp = Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
            return timestamp.getSeconds() + ":" + timestamp.getNanos() + ":" + member.uid();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 멤버 상세 조회 (공개)
     * @param uid 멤버 UID
     * @return MemberDTO (ANONYMIZED 상태면 익명화된 필드만 반환)
     * @throws BusinessException MEMBER_NOT_FOUND: uid에 해당하는 멤버 없음
     */
    /**
     * 멤버 상세 조회 (공개)
     */
    public MemberDTO getMemberByUid(String uid) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            if (db == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore가 초기화되지 않았습니다.");
            }

            ApiFuture<com.google.cloud.firestore.DocumentSnapshot> future = db.collection("members")
                    .document(uid)
                    .get();

            com.google.cloud.firestore.DocumentSnapshot document = future.get();

            if (!document.exists()) {
                throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND, "멤버를 찾을 수 없습니다: " + uid);
            }

            MemberDTO member = convertDocumentToMemberDTO(document);
            if (member == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "데이터 오류");
            }

            // 탈퇴 회원 -> 없는 사람 취급 (404)
            if (MemberStatus.ANONYMIZED.equals(member.status())) {
                throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND, "탈퇴한 회원입니다.");
            }

            return member;
        } catch (InterruptedException | ExecutionException e) {
            log.error("멤버 조회 실패 - uid: {}", uid, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "멤버 조회 중 오류가 발생했습니다.");
        }
    }


    /**
     * 멤버 익명화 처리
     * ANONYMIZED 상태일 때 PII 필드 제거/익명화
     */
    public void anonymizeMember(String uid) {
        Firestore db = FirestoreClient.getFirestore();

        try {
            // 1. 스킬 역인덱스 정리를 위해 먼저 기존 정보를 조회
            // (그냥 지워버리면 어떤 스킬에 이 사람이 등록돼 있었는지 알 수 없으므로)
            DocumentSnapshot document = db.collection("members").document(uid).get().get();

            if (!document.exists()) {
                throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND, "존재하지 않는 회원입니다.");
            }

            // 2. 탈퇴 전 기수+파트 정보 저장 (게시글 authorName 업데이트용)
            String generation = document.getString("generation");
            String partStr = document.getString("part");
            Part part = null;
            if (partStr != null && !partStr.isEmpty()) {
                try {
                    part = Part.parse(partStr);
                } catch (Exception e) {
                    log.warn("Part 변환 실패 - part: {}, error: {}", partStr, e.getMessage());
                }
            }

            // 3. 'skill_members' (역인덱스) 컬렉션에서 내 정보 제거
            // 예: Java 검색 결과에 탈퇴한 회원이 나오면 안 되니까 제거
            List<String> skillIds = FirestoreUtils.asStringList(document.get("skillIds"));
            if (skillIds != null) {
                for (String skillId : skillIds) {
                    try {
                        db.collection("skill_members")
                                .document(skillId)
                                .collection("members")
                                .document(uid)
                                .delete(); // Fire-and-forget (비동기 삭제)
                    } catch (Exception e) {
                        log.warn("스킬 역인덱스 삭제 실패 - skillId: {}, error: {}", skillId, e.getMessage());
                    }
                }
            }

            // 4. 본문(members) 문서 익명화 (개인정보 null 처리)
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", MemberStatus.ANONYMIZED.name()); // 상태 변경
            updates.put("name", "탈퇴회원"); // 이름 변경 (또는 "알수없음")
            updates.put("bio", null);      // 소개글 삭제
            updates.put("socialLinks", null); // 소셜 링크 삭제
            updates.put("github", null);   // 깃허브 정보(사진 등) 삭제
            updates.put("skillIds", null); // 스킬 목록 삭제
            updates.put("updatedAt", Timestamp.now());

            // update() 메서드는 Map에 있는 필드만 변경하고, 나머지는 유지합니다.
            // (기수나 파트는 통계용으로 남겨둘 수도 있습니다. 지우려면 null 추가하세요)
            db.collection("members").document(uid).update(updates).get();

            // 5. 해당 멤버가 작성한 게시글들의 authorName을 "기수+파트" 형식으로 업데이트
            if (generation != null && part != null) {
                updatePostAuthorNames(db, uid, generation, part);
            } else {
                log.warn("기수 또는 파트 정보가 없어 게시글 authorName을 업데이트할 수 없습니다 - uid: {}, generation: {}, part: {}", 
                        uid, generation, part);
            }

            log.info("회원 탈퇴(익명화) 완료 - uid: {}", uid);

        } catch (Exception e) {
            log.error("회원 탈퇴 처리 실패 - uid: {}", uid, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "회원 탈퇴 처리에 실패했습니다.");
        }
    }

    /**
     * 탈퇴한 멤버가 작성한 게시글들의 authorName을 "기수+파트" 형식으로 업데이트
     * 예: "5기 WEB·FE"
     */
    private void updatePostAuthorNames(Firestore db, String authorId, String generation, Part part) {
        try {
            // 해당 멤버가 작성한 모든 게시글 조회
            Query query = db.collection("posts")
                    .whereEqualTo("authorId", authorId);
            
            QuerySnapshot snapshot = query.get().get();
            List<QueryDocumentSnapshot> posts = snapshot.getDocuments();
            
            if (posts.isEmpty()) {
                log.info("업데이트할 게시글이 없습니다 - authorId: {}", authorId);
                return;
            }
            
            // 기수+파트 형식으로 authorName 생성
            String authorName = generation + " " + part.wireValue(); // 예: "5기 WEB·FE"
            
            // 모든 게시글의 authorName 업데이트
            int updatedCount = 0;
            for (QueryDocumentSnapshot postDoc : posts) {
                try {
                    postDoc.getReference().update("authorName", authorName).get();
                    updatedCount++;
                } catch (Exception e) {
                    log.warn("게시글 authorName 업데이트 실패 - postId: {}, error: {}", 
                            postDoc.getId(), e.getMessage());
                }
            }
            
            log.info("게시글 authorName 업데이트 완료 - authorId: {}, 업데이트된 게시글 수: {}, authorName: {}", 
                    authorId, updatedCount, authorName);
                    
        } catch (Exception e) {
            // 게시글 업데이트 실패해도 탈퇴 처리는 계속 진행
            log.error("게시글 authorName 업데이트 중 오류 발생 - authorId: {}, error: {}", authorId, e.getMessage());
        }
    }

    /**
     * pageSize 검증
     */
    private int validatePageSize(Integer pageSize) {
        if (pageSize == null) {
            return defaultPageSize;
        }
        if (pageSize < 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "pageSize는 1 이상이어야 합니다.");
        }
        if (pageSize > maxPageSize) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "pageSize는 " + maxPageSize + " 이하여야 합니다.");
        }
        return pageSize;
    }

    /**
     * Firestore DocumentSnapshot을 MemberDTO로 변환
     * Part enum 변환 문제를 해결하기 위해 수동 변환 사용
     */
    private MemberDTO convertDocumentToMemberDTO(DocumentSnapshot document) {
        try {
            String uid = document.getId();
            String name = document.getString("name");
            String generation = document.getString("generation");
            
            // Part enum 변환 - Firestore에 저장된 문자열을 Part.parse()로 변환
            Part part = null;
            String partStr = document.getString("part");
            if (partStr != null && !partStr.isEmpty()) {
                try {
                    part = Part.parse(partStr);
                } catch (Exception e) {
                    log.warn("Part 변환 실패 - part: {}, error: {}", partStr, e.getMessage());
                    return null;
                }
            }
            
            String bio = document.getString("bio");
            
            // SocialLinks 변환
            List<SocialLink> socialLinks = null;
            List<Map<String, Object>> rawLinks = FirestoreUtils.asMapList(document.get("socialLinks"));
            if (rawLinks != null && !rawLinks.isEmpty()) {
                socialLinks = new ArrayList<>();
                for (Map<String, Object> link : rawLinks) {
                    try {
                        String typeStr = String.valueOf(link.get("type"));
                        String url = String.valueOf(link.get("url"));
                        socialLinks.add(new SocialLink(SocialLinkType.valueOf(typeStr), url));
                    } catch (Exception e) {
                        log.warn("SocialLink 변환 실패 - link: {}, error: {}", link, e.getMessage());
                    }
                }
            }
            
            // SkillIds 변환
            List<String> skillIds = FirestoreUtils.asStringList(document.get("skillIds"));
            
            // Github 변환
            GithubDTO github = null;
            Map<String, Object> githubMap = FirestoreUtils.asMap(document.get("github"));
            if (githubMap != null) {
                String username = String.valueOf(githubMap.get("username"));
                String photoUrl = String.valueOf(githubMap.get("photoUrl"));
                if (!"null".equals(username) || !"null".equals(photoUrl)) {
                    github = new GithubDTO(
                            "null".equals(username) ? null : username,
                            "null".equals(photoUrl) ? null : photoUrl
                    );
                }
            }
            
            // Status 변환
            MemberStatus status = MemberStatus.ACTIVE;
            String statusStr = document.getString("status");
            if (statusStr != null) {
                try {
                    status = MemberStatus.valueOf(statusStr);
                } catch (Exception e) {
                    log.warn("MemberStatus 변환 실패 - status: {}, error: {}", statusStr, e.getMessage());
                }
            }
            
            // Timestamp를 String으로 변환
            String createdAt = FirestoreUtils.toIsoString(document.getTimestamp("createdAt"));
            String updatedAt = FirestoreUtils.toIsoString(document.getTimestamp("updatedAt"));
            
            return new MemberDTO(
                    uid,
                    name,
                    generation,
                    part,
                    bio,
                    socialLinks,
                    skillIds,
                    github,
                    status,
                    createdAt,
                    updatedAt
            );
        } catch (Exception e) {
            log.error("MemberDTO 변환 실패 - documentId: {}, error: {}", document.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * MemberDTO를 MemberListItemDTO로 변환
     * bioShort와 socialSummary 생성 포함
     */
    private MemberListItemDTO convertToListItem(MemberDTO member) {
        // bioShort 생성 (bio 앞 80자)
        String bioShort = null;
        if (member.bio() != null && !member.bio().isEmpty()) {
            bioShort = member.bio().length() > 80 
                    ? member.bio().substring(0, 80) 
                    : member.bio();
        }

        // socialSummary 생성 -> 이렇게 넘기면 fe에서 링크 대신 존재 여부만 받습니다.(삭제)
        //        boolean hasGithub = false;
        //        boolean hasLinkedin = false;
        //        boolean hasInstagram = false;
        //
        //        if (member.socialLinks() != null) {
        //            for (SocialLink link : member.socialLinks()) {
        //                if (link.type() == SocialLinkType.GITHUB) {
        //                    hasGithub = true;
        //                } else if (link.type() == SocialLinkType.LINKEDIN) {
        //                    hasLinkedin = true;
        //                } else if (link.type() == SocialLinkType.INSTAGRAM) {
        //                    hasInstagram = true;
        //                }
        //            }
        //        }
        //
        //        SocialSummary socialSummary = new SocialSummary(hasGithub, hasLinkedin, hasInstagram);

        return new MemberListItemDTO(
                member.uid(),
                member.name(),
                member.generation(),
                member.part(),
                member.skillIds(),
                member.github(),
                bioShort,
                member.socialLinks(),
                member.status()
        );
    }

    // 기존 메서드 (하위 호환성 유지)
    public List<MemberDTO> getAllMembers() {
        MemberListResponse response = getMembers(null, null, null, null, null, null);
        return response.items().stream()
                .map(item -> {
                    // MemberListItemDTO를 MemberDTO로 역변환 (기존 코드 호환성)
                    // 이 메서드는 사용되지 않을 수 있으므로 간단한 변환만 수행
                    return new MemberDTO(
                            item.uid(),
                            item.name(),
                            item.generation(),
                            item.part(),
                            null, // bio는 리스트에 없음
                            item.socialLinks(),
                            item.skillIds(),
                            item.github(),
                            item.status(),
                            null, // createdAt
                            null  // updatedAt
                    );
                })
                .collect(Collectors.toList());
    }

    public String signUp(MemberDTO memberDTO) {
        Firestore db = FirestoreClient.getFirestore();

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("uid", memberDTO.uid());
            data.put("name", memberDTO.name());
            data.put("generation", memberDTO.generation());
            data.put("part", memberDTO.part().name());
            data.put("bio", memberDTO.bio());

            // SocialLink 리스트 변환 (DTO -> Map)
            if (memberDTO.socialLinks() != null) {
                List<Map<String, String>> links = memberDTO.socialLinks().stream()
                        .map(link -> Map.of("type", link.type().name(), "url", link.url()))
                        .collect(Collectors.toList());
                data.put("socialLinks", links);
            }

            data.put("skillIds", memberDTO.skillIds());

            // Github DTO 변환
            if (memberDTO.github() != null) {
                data.put("github", Map.of(
                        "username", memberDTO.github().username(),
                        "photoUrl", memberDTO.github().photoUrl()
                ));
            }

            data.put("status", MemberStatus.ACTIVE.name());
            data.put("createdAt", Timestamp.now());
            data.put("updatedAt", Timestamp.now());

            db.collection("members").document(memberDTO.uid()).set(data);

            return "회원가입 성공: " + memberDTO.name();

        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "회원가입 실패: " + e.getMessage());
        }
    }

    // 프로필 수정 (추가)
    public MemberDTO updateMyProfile(String uid, MeUpdateRequest request) {
        Firestore db = FirestoreClient.getFirestore();
        try {
            Map<String, Object> updates = new HashMap<>();
            if (request.bio() != null) {
                updates.put("bio", request.bio());
            }
            if (request.socialLinks() != null) {
                List<Map<String, String>> linkMaps = request.socialLinks().stream()
                        .map(req -> {
                            try {
                                SocialLinkType.valueOf(req.type());
                            } catch (IllegalArgumentException e) {
                            }
                            return Map.of(
                                    "type", req.type(),
                                    "url", req.url()
                            );
                        })
                        .collect(Collectors.toList());
                updates.put("socialLinks", linkMaps);
            }

            // SkillIds 업데이트
            if (request.skillIds() != null) {
                updates.put("skillIds", request.skillIds());
            }

            updates.put("updatedAt", Timestamp.now());

            // Firestore 업데이트
            db.collection("members").document(uid).update(updates).get();
            return getMemberByUid(uid);

        } catch (Exception e) {
            log.error("프로필 업데이트 실패 - uid: {}", uid, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "프로필 수정 중 오류가 발생했습니다.");
        }
    }

    public MeBootstrapResponse bootstrap(String uid, MeBootstrapRequest request, String photoUrl) {
        Firestore db = FirestoreClient.getFirestore();

        try {
            // 1. Part 문자열 -> Enum 변환
            Part part = Part.parse(request.part());

            // 2. MemberDTO 생성 (초기값 세팅)
            MemberDTO newMember = new MemberDTO(
                    uid,
                    request.name(),
                    request.generation(),
                    part,
                    "반갑습니다!", // 기본 Bio
                    new ArrayList<>(), // SocialLinks 빈 리스트
                    new ArrayList<>(), // SkillIds 빈 리스트
                    new GithubDTO(null, photoUrl), // Github 정보는 사진만
                    MemberStatus.ACTIVE,
                    FirestoreUtils.toIsoString(Timestamp.now()), // createdAt
                    FirestoreUtils.toIsoString(Timestamp.now())  // updatedAt
            );

            // 3. DB 저장 (기존 로직 활용)
            // 아래 saveLogic은 signUp 메서드 내용을 별도 메서드로 뺐다고 가정하거나, 직접 구현
            Map<String, Object> data = convertMemberDtoToMap(newMember); // 별도 유틸 메서드 필요
            data.put("createdAt", Timestamp.now());
            data.put("updatedAt", Timestamp.now());

            db.collection("members").document(uid).set(data).get();

            // 4. 응답 생성 (isProfileComplete = true)
            return new MeBootstrapResponse(newMember, true);

        } catch (Exception e) {
            log.error("Bootstrap 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "초기 프로필 설정 실패");
        }
    }

    // (참고) DTO -> Map 변환 헬퍼
    private Map<String, Object> convertMemberDtoToMap(MemberDTO member) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", member.uid());
        data.put("name", member.name());
        data.put("generation", member.generation());
        data.put("part", member.part().name());
        data.put("bio", member.bio());
        data.put("socialLinks", Collections.emptyList());
        data.put("skillIds", Collections.emptyList());
        data.put("github", Map.of("photoUrl", member.github().photoUrl()));
        data.put("status", member.status().name());
        return data;
    }

    // (MeController용)프로필 수정
    public MemberDTO updateMe(String uid, String bio, List<SocialLinkRequest> socialLinks, List<String> skillIds) {
        Firestore db = FirestoreClient.getFirestore();

        try {
            Map<String, Object> updates = new HashMap<>();

            // 1. Bio 업데이트 (null이 아닐 때만 업데이트하려면 null 체크 추가)
            // 프론트에서 값을 지우려고 빈 문자열 ""을 보낼 수도 있으므로 그대로 put
            if (bio != null) {
                updates.put("bio", bio);
            }

            // 2. SocialLinks 변환 (Request DTO List -> Map List)
            // Firestore는 커스텀 객체 리스트 저장이 까다로우므로 Map List로 변환하여 저장
            if (socialLinks != null) {
                List<Map<String, String>> linkMaps = socialLinks.stream()
                        .map(link -> {
                            // (선택) Enum 유효성 검증 로직
                            // try { SocialLinkType.valueOf(link.type()); } catch ...

                            return Map.of(
                                    "type", link.type(),
                                    "url", link.url()
                            );
                        })
                        .collect(Collectors.toList());
                updates.put("socialLinks", linkMaps);
            }

            // 3. SkillIds 업데이트
            if (skillIds != null) {
                updates.put("skillIds", skillIds);

                // [심화] 만약 스킬별 검색(역인덱스) 기능을 쓴다면,
                // 여기서 skill_members 컬렉션도 같이 갱신해줘야 데이터가 꼬이지 않습니다.
                // 일단은 member 문서만 업데이트합니다.
            }

            // 4. 수정 시간 갱신
            updates.put("updatedAt", Timestamp.now());

            // 5. Firestore 업데이트 실행 (update는 해당 필드만 변경, set은 덮어쓰기)
            db.collection("members").document(uid).update(updates).get();

            // 6. 업데이트된 최신 정보를 조회해서 반환
            return getMemberByUid(uid);

        } catch (Exception e) {
            log.error("내 정보 수정 실패 - uid: {}, error: {}", uid, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "내 정보 수정에 실패했습니다.");
        }
    }

}