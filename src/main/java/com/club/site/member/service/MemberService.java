package com.club.site.member.service;

import com.club.site.member.dto.MemberDTO;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class MemberService {

    // 컨트롤러가 호출하는 그 메서드!
    public String saveMockData() {
        Firestore db = FirestoreClient.getFirestore();
        List<MemberDTO> mockList = createMockMembers(); // 아래에서 데이터 생성

        int count = 0;
        for (MemberDTO member : mockList) {
            try {
                db.collection("members").document(member.getUid()).set(member);
                count++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count + "명의 멤버 저장 완료!";
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
                .github(new MemberDTO.GithubInfo("yeomine"))
                .socialLinks(List.of(new MemberDTO.SocialLink("BLOG", "https://velog.io/@yjw326/posts")))
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

        // 대훈, 민석, 채영
        list.add(MemberDTO.builder().uid("3").name("권대훈").part("AI").generation("5기").status("ACTIVE").bio("AI가 세상을 지배한다").createdAt(now).build());
        list.add(MemberDTO.builder().uid("4").name("최민석").part("Design").generation("5기").status("ACTIVE").bio("하이하이~~").createdAt(now).build());
        list.add(MemberDTO.builder().uid("5").name("임채영").part("WEB-BE").generation("5기").status("ACTIVE").bio("서버 짓는 여인").createdAt(now).build());

        return list;
    }

    public List<MemberDTO> getAllMembers() {
        Firestore db = FirestoreClient.getFirestore();
        List<MemberDTO> list = new ArrayList<>();

        try {
            ApiFuture<QuerySnapshot> future = db.collection("members").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                // 3. 문서를 DTO로 변환 (toObject 사용)
                MemberDTO member = document.toObject(MemberDTO.class);
                list.add(member);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            // 에러 나면 빈 리스트 리턴
        }

        return list;
    }
}

