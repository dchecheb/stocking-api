package com.stocking.modules.todayword;

import com.stocking.infra.common.FirebaseUser;
import com.stocking.modules.todayword.repo.TodayWord;
import com.stocking.modules.todayword.repo.TodayWordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
class TodayWordServiceTest {

    @Autowired
    private TodayWordService todayWordService;

    @Autowired
    private TodayWordRepository todayWordRepository;

    @DisplayName("단어 저장 테스트")
    @Test
    void saveTodayWordTest() {
        //given
        //when
        //then
    }

    @DisplayName("좋아요/안좋아요 (존재여부 체크함) 테스트")
    @Test
    void saveTodayWordLikeTest() {
        //given
        //when
        //then
    }

    @DisplayName("좋아요가 가장많은 오늘의 단어 (사용자 정보 넘어오면 사용자가 좋아요했는지도 확인해줌) 테스트")
    @Test
    void getTopWordTest() {
        //given
        //when
        //then
    }

    @DisplayName("오늘의 단어 조회")
    @Test
    @Transactional
    void getTodayWordTest() {
        //given
        FirebaseUser testUser;
        TodayWord todayWord;

        //when
        testUser = FirebaseUser.builder()
                .uid("123123123")
                .name("user")
                .email("user@test.com")
                .build();

        todayWord = TodayWord.builder()
                .createdUid(testUser.getUid())
                .wordName("테스트")
                .mean("테스트한다는뜻")
                .build();

        todayWordRepository.save(todayWord);

        //then
//        Optional<TodayWord> result = todayWordService.getTodayWord(testUser, todayWord.getId());
//        assertEquals(result.get().getWordName(), todayWord.getWordName());
    }

    @DisplayName("오늘의 단어 수정 테스트")
    @Test
    @Transactional
    void updateTodayWordTest() {
        //given
        //when
        //then
    }

    @DisplayName("최근 기준으로 등록된 오늘의 단어 목록 조회 테스트")
    @Test
    void getRecentlyTodayWordSortListTest() {
        //given
        //when
        //then
    }
}