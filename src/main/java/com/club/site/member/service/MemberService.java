package com.club.site.member.service;

import com.club.site.common.exception.BusinessException;
import com.club.site.common.error.ErrorCode;
import com.club.site.member.dto.GithubDTO;
import com.club.site.member.dto.MemberDTO;
import com.club.site.member.dto.MemberListResponse;
import com.club.site.member.dto.MemberListItemDTO;
import com.club.site.member.dto.SocialSummary;
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
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "멤버 데이터를 파싱할 수 없습니다.");
            }

            // ANONYMIZED 상태면 익명화 처리
            if (MemberStatus.ANONYMIZED.equals(member.status())) {
                return anonymizeMember(member);
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
    private MemberDTO anonymizeMember(MemberDTO member) {
        // MemberDTO는 record이므로 새 인스턴스 생성
        return new MemberDTO(
                member.uid(),
                "탈퇴회원",
                member.generation(), // 기수는 유지 가능 (정책에 따라)
                member.part(), // 파트는 유지 가능 (정책에 따라)
                null, // bio 제거
                null, // socialLinks 제거
                null, // skillIds 제거
                null, // github 정보 제거 (PII)
                MemberStatus.ANONYMIZED,
                member.createdAt(),
                member.updatedAt()
        );
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
}