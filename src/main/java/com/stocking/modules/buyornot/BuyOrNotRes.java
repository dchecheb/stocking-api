package com.stocking.modules.buyornot;

import java.util.List;

import com.stocking.infra.common.PageInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuyOrNotRes {

    private List<SimpleEvaluation> evaluationList;
    
    private PageInfo pageInfo;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimpleEvaluation {
        
        private int id;
        private String code;        // 종목코드
        private String company;     // 회사명
        private String pros;        // 장점
        private String cons;        // 단점
        private String uuid;        // 등록자 uid
        private long likeCount;     // 좋아요 개수
        
    }

}
