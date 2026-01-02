package com.club.site.news.service;

import com.club.site.news.dto.NewsDTO;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class NewsService {

    private static final String COLLECTION_NAME = "news";

    public String saveMockNews() {
        Firestore db = FirestoreClient.getFirestore();
        List<NewsDTO> mockList = createMockNews();
        int count = 0;

        for (NewsDTO news : mockList) {
            try {
                db.collection(COLLECTION_NAME).document(news.getId()).set(news);
                count++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count + "개의 뉴스 데이터 저장 완료!";
    }

    public List<NewsDTO> getAllNews() {
        Firestore db = FirestoreClient.getFirestore();
        List<NewsDTO> list = new ArrayList<>();

        try {
            ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                try {
                    NewsDTO news = document.toObject(NewsDTO.class);
                    if (news.getId() == null) news.setId(document.getId());
                    list.add(news);
                } catch (Exception e) {
                    System.err.println("뉴스 데이터 변환 실패 (무시됨): " + document.getId());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 🔥 [수정] 날짜를 다양하게 넣은 Mock 데이터 생성
    private List<NewsDTO> createMockNews() {
        List<NewsDTO> list = new ArrayList<>();

        // 1. 신입 모집 -> 2월 1일
        list.add(NewsDTO.builder()
                .id("news-001")
                .title("GDGOC SeoulTech 5기 신입부원 모집")
                .type("NOTICE")
                .date(makeDate(2026, 2, 1)) // 2월 1일로 설정
                .imageUrl("https://images.unsplash.com/photo-1531482615713-2afd69097998?w=500&auto=format&fit=crop")
                .content("드디어 5기 모집이 시작되었습니다! 개발에 열정 있는 분들을 기다립니다.")
                .body("안녕하세요! GDGOC SeoulTech입니다.\n\n" +
                        "드디어 2026년도 5기 신입 부원 모집을 시작합니다!\n" +
                        "저희는 Google 기술을 활용하여 다양한 프로젝트와 스터디를 진행합니다.\n\n" +
                        "✅ 모집 기간: 2월 1일 ~ 2월 14일\n" +
                        "✅ 모집 분야: Web(FE/BE), App, AI, Design\n\n" +
                        "열정 있는 여러분의 많은 지원 바랍니다!")
                .build());

        // 2. 솔루션 챌린지 -> 1월 26일 (이번 달 행사)
        list.add(NewsDTO.builder()
                .id("news-002")
                .title("2026 Google Solution Challenge 킥오프")
                .type("EVENT")
                .date(makeDate(2026, 1, 26)) // 1월 26일로 설정
                .imageUrl("https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=500&auto=format&fit=crop")
                .content("구글 기술로 지역 사회의 문제를 해결해보세요. 글로벌 해커톤이 시작됩니다.")
                .body("Solution Challenge는 UN의 지속가능발전목표(SDGs)를 해결하는 해커톤입니다.\n\n" +
                        "전 세계 학생들과 경쟁하며 실력을 증명하세요!\n" +
                        "우승 팀에게는 구글 본사 멘토링이 주어집니다.")
                .build());

        // 3. 정기 세션 -> 2월 20일
        list.add(NewsDTO.builder()
                .id("news-003")
                .title("2월 정기 세션: Spring Boot Deep Dive")
                .type("TECH")
                .date(makeDate(2026, 2, 20)) // 2월 20일로 설정
                .imageUrl("https://images.unsplash.com/photo-1605379399642-870262d3d051?w=500&auto=format&fit=crop")
                .content("오거나이저 염정우님의 심도 깊은 스프링 부트 강의가 있습니다.")
                .body("이번 주제는 'Spring Boot 실무 적용'입니다.\n\n" +
                        "발표자: 염정우\n" +
                        "일시: 2월 20일 19:00\n" +
                        "장소: 프론티어관 302호\n\n" +
                        "노트북을 지참해주세요.")
                .build());

        return list;
    }

    // 날짜 생성 헬퍼
    private Date makeDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day); // 자바 Calendar의 월은 0부터 시작
        return cal.getTime();
    }
}