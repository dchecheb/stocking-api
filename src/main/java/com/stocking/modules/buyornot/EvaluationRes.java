package com.stocking.modules.buyornot;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationRes {

    private List<Evaluation> evaluationList;
    
    private PageInfo pageInfo;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Evaluation {
        
        private int id;
        private String code;        // 종목코드
        private String company;     // 회사명
        private String pros;        // 장점
        private String cons;        // 단점
        private String giphyImgId;  // giphy 이미지
        private long likeCount;     // 좋아요 개수
        private boolean userlike;
        
        // recentComment 객체 하나 있어야함.
        // comment 갯수
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PageInfo {
        
        private int pageSize;
        private int pageNo;
        private long count;
    }

}
