package com.club.site.member.service;

import com.club.site.common.exception.BusinessException;
import com.club.site.common.error.ErrorCode;
import com.club.site.member.dto.MemberDTO;
import com.club.site.member.dto.MemberListResponse;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
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
                // 1. 멤버 저장
                db.collection("members").document(member.getUid()).set(member);
                count++;

                // 2. 역인덱스 생성 (skillIds가 있는 경우)
                if (member.getSkillIds() != null && !member.getSkillIds().isEmpty()) {
                    for (String skillId : member.getSkillIds()) {
                        try {
                            Map<String, Object> indexData = new HashMap<>();
                            indexData.put("uid", member.getUid());
                            indexData.put("createdAt", Timestamp.now());

                            db.collection("skill_members")
                                    .document(skillId)
                                    .collection("members")
                                    .document(member.getUid())
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

        // 정우
        list.add(MemberDTO.builder()
                .uid("1")
                .name("염정우")
                .part("App")
                .generation("5기")
                .status("ACTIVE")
                .bio("GDGOC가 터지면 제 탓입니다.")
                .introduction("백엔드 개발을 주로 담당하고 있습니다.")
                .photoUrl("https://github.com/yeomine.png")
                .github(MemberDTO.GithubInfo.builder()
                        .username("yeomine")
                        .photoUrl("https://github.com/yeomine.png")
                        .build())
                .socialLinks(List.of(new MemberDTO.SocialLink("BLOG", "https://velog.io/@yjw326/posts")))
                .skillIds(List.of("Spring Boot", "Java", "Kotlin"))
                .createdAt(now).updatedAt(now)
                .build());

        // 가연
        list.add(MemberDTO.builder()
                .uid("2")
                .name("이가연")
                .part("WEB-FE")
                .generation("5기")
                .status("ACTIVE")
                .bio("프론트엔드 깎는 장인")
                .introduction("React와 Vue를 좋아합니다.")
                .skillIds(List.of("React", "Vue", "Tailwind"))
                .createdAt(now).updatedAt(now)
                .build());

        // 대훈
        list.add(MemberDTO.builder()
                .uid("3")
                .name("권대훈")
                .part("AI")
                .generation("5기")
                .status("ACTIVE")
                .bio("AI가 세상을 지배한다")
                .skillIds(List.of("Python", "TensorFlow", "React")) // React 공통 스킬 추가 (AND 필터 테스트용)
                .createdAt(now)
                .updatedAt(now)
                .build());

        // 민석
        list.add(MemberDTO.builder()
                .uid("4")
                .name("최민석")
                .part("Design")
                .generation("5기")
                .status("ACTIVE")
                .bio("하이하이~~")
                .skillIds(List.of("Figma", "React")) // React 공통 스킬 추가 (AND 필터 테스트용)
                .createdAt(now)
                .updatedAt(now)
                .build());

        // 채영
        list.add(MemberDTO.builder()
                .uid("5")
                .name("임채영")
                .part("WEB-BE")
                .generation("5기")
                .status("ACTIVE")
                .bio("서버 짓는 여인")
                .skillIds(List.of("Spring Boot", "Java", "React")) // React 공통 스킬 추가 (AND 필터 테스트용)
                .createdAt(now)
                .updatedAt(now)
                .build());

        return list;
    }

    /**
     * 멤버 리스트 조회 (필터 + 페이지네이션)
     */
    public MemberListResponse getMembers(
            String generation,
            String part,
            List<String> skillIds,
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
                members = getMembersBySkillIds(db, skillIds, generation, part);
            } else {
                // 4. skillIds가 없으면 일반 쿼리
                members = getMembersByQuery(db, generation, part);
            }

            // 5. 정렬 (createdAt desc)
            members.sort((a, b) -> {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt()); // desc
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

            return MemberListResponse.builder()
                    .items(pagedMembers)
                    .nextCursor(nextCursor)
                    .build();

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
            String part
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
                    MemberDTO member = document.toObject(MemberDTO.class);
                    if (member != null) {
                        // 4. generation/part 필터 적용
                        if ((generation == null || generation.equals(member.getGeneration())) &&
                            (part == null || part.equals(member.getPart()))) {
                            members.add(member);
                        }
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
            String part
    ) throws InterruptedException, ExecutionException {
        Query query = db.collection("members");

        // generation 필터
        if (generation != null && !generation.isEmpty()) {
            query = query.whereEqualTo("generation", generation);
        }

        // part 필터
        if (part != null && !part.isEmpty()) {
            query = query.whereEqualTo("part", part);
        }

        // 쿼리 실행
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<MemberDTO> members = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            MemberDTO member = document.toObject(MemberDTO.class);
            if (member != null) {
                members.add(member);
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
                        if (member.getCreatedAt() == null) return false;
                        int compare = member.getCreatedAt().compareTo(cursorTimestamp);
                        if (compare > 0) return true; // createdAt이 cursor보다 이후 (더 최신)
                        if (compare < 0) return false; // createdAt이 cursor보다 이전
                        // createdAt이 같으면 uid로 비교 (desc 정렬이므로 uid가 더 작은 것이 다음)
                        return member.getUid() != null && member.getUid().compareTo(cursorUid) < 0;
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
        if (member.getCreatedAt() == null || member.getUid() == null) {
            return null;
        }
        Timestamp timestamp = member.getCreatedAt();
        return timestamp.getSeconds() + ":" + timestamp.getNanos() + ":" + member.getUid();
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

            MemberDTO member = document.toObject(MemberDTO.class);
            if (member == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "멤버 데이터를 파싱할 수 없습니다.");
            }

            // ANONYMIZED 상태면 익명화 처리
            if ("ANONYMIZED".equals(member.getStatus())) {
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
        MemberDTO anonymized = new MemberDTO();
        anonymized.setUid(member.getUid());
        anonymized.setName("탈퇴회원");
        anonymized.setGeneration(member.getGeneration()); // 기수는 유지 가능 (정책에 따라)
        anonymized.setPart(member.getPart()); // 파트는 유지 가능 (정책에 따라)
        anonymized.setStatus("ANONYMIZED");
        anonymized.setBio(null); // bio 제거
        anonymized.setPhotoUrl(null); // photoUrl 제거
        anonymized.setSocialLinks(null); // socialLinks 제거
        anonymized.setSkillIds(null); // skillIds 제거
        anonymized.setGithub(null); // github 정보 제거 (PII)
        anonymized.setCreatedAt(member.getCreatedAt());
        anonymized.setUpdatedAt(member.getUpdatedAt());
        anonymized.setIntroduction(null); // introduction 제거
        return anonymized;
    }

    // 기존 메서드 (하위 호환성 유지)
    public List<MemberDTO> getAllMembers() {
        MemberListResponse response = getMembers(null, null, null, null, null);
        return response.getItems();
    }
}
