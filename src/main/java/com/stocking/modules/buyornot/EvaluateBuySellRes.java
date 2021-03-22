package com.stocking.modules.buyornot;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateBuySellRes {
    
    @ApiModelProperty(notes = "종목코드", position = 1)
    private String code;

    @ApiModelProperty(notes = "살래 갯수", position = 2)
    private long buyCount;
    
    @ApiModelProperty(notes = "말래 개수", position = 3)
    private long sellCount;
    
    @ApiModelProperty(notes = "사용자의 살래 말래 선택 값", position = 4)
    private BuySell userBuySell;
}
