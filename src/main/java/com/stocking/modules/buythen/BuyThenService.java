package com.stocking.modules.buythen;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.stocking.infra.common.FirebaseUser;
import com.stocking.infra.common.PageInfo;
import com.stocking.infra.common.PageParam;
import com.stocking.infra.common.StockUtils;
import com.stocking.infra.common.StockUtils.RealTimeStock;
import com.stocking.modules.buythen.CalcHistRes.CalculationHist;
import com.stocking.modules.buythen.CalculatedRes.CalculatedValue;
import com.stocking.modules.buythen.CurrentKospiIndustryRes.CurrentValue;
import com.stocking.modules.buythen.CurrentKospiIndustryRes.IndustryValue;
import com.stocking.modules.buythen.CurrentKospiIndustryRes.KospiValue;
import com.stocking.modules.buythen.StockRes.Company;
import com.stocking.modules.buythen.repo.CalcHist;
import com.stocking.modules.buythen.repo.CalcHistRepository;
import com.stocking.modules.buythen.repo.StocksPrice;
import com.stocking.modules.buythen.repo.StocksPriceRepository;
import com.stocking.modules.stock.Stock;
import com.stocking.modules.stock.StockRepository;

@Service
public class BuyThenService {
	private static final String FORMAT = "yyyy-MM-dd HH:mm:ss"; 

    @Autowired
    private StockRepository stockRepository;
    
    @Autowired
    private StocksPriceRepository stocksPriceRepository;
    
    @Autowired
    private StockUtils stockUtils;
    
    @Autowired
    private CalcHistRepository calcHistRepository;
    
    /**
     * kospi 상장기업 전체 조회
     * @return
     * @throws Exception 
     */
    public StockRes getStockList() throws Exception {
        
        List<Company> resultList = stockRepository.findAllByMarket("KS")
            .orElseThrow(() -> new Exception("조회 실패"))
            .stream().map(vo -> 
                Company.builder()
                    .company(vo.getCompany())
                    .code(vo.getCode())
                    .build()
            ).collect(Collectors.toList());
        
        return StockRes.builder()
            .companyList(resultList)
            .count(resultList.size())
            .build();
    }
    
    /**
     * 계산기
     * @param buyThenForm
     * @return
     * @throws Exception
     */
    public CalculatedRes getPastStock(BuyThenForm buyThenForm, FirebaseUser user) throws Exception {
        Stock stock = stockRepository.findByCode(buyThenForm.getCode())
                .orElseThrow(() -> new Exception("종목코드가 올바르지 않습니다."));
        
        StocksPrice stockPrice = stocksPriceRepository.findByStocksId(stock.getId())
                .orElseThrow(() -> new Exception("종목코드가 올바르지 않습니다."));
        
        String code = stock.getCode();
        InvestDate investDate = buyThenForm.getInvestDate();
        BigDecimal investPrice = buyThenForm.getInvestPrice();    // 투자금

        // 과거 주가
        BigDecimal oldStockPrice = Optional.ofNullable( switch (investDate) {
            case DAY1 -> stockPrice.getPrice();
            case WEEK1 -> stockPrice.getPriceW1();
            case MONTH1 -> stockPrice.getPriceM1();
            case MONTH6 -> stockPrice.getPriceM6();
            case YEAR1 -> stockPrice.getPriceY1();
            case YEAR5 -> stockPrice.getPriceY5();
            case YEAR10 -> stockPrice.getPriceY10();
            default -> throw new IllegalArgumentException("Unexpected value: " + investDate);
        }).orElseThrow(() -> new Exception(stock.getCompany() + " 는(은) " + investDate.getName() + " 데이터가 없습니다."));
        
        // 종가일자
        LocalDateTime oldCloseDate = switch (investDate) {    
            case DAY1 -> stockPrice.getLastTradeDate();
            case WEEK1 -> stockPrice.getDateW1();
            case MONTH1 -> stockPrice.getDateM1();
            case MONTH6 -> stockPrice.getDateM6();
            case YEAR1 -> stockPrice.getDateY1();
            case YEAR5 -> stockPrice.getDateY5();
            case YEAR10 -> stockPrice.getDateY10();
            default -> throw new IllegalArgumentException("Unexpected value: " + investDate);
        };

        RealTimeStock realTimeStock = stockUtils.getStockInfo(code);
        
        BigDecimal currentPrice = realTimeStock.getCurrentPrice(); // 현재가 - 실시간정보 호출
        String lastTradeTime = realTimeStock.getLastTradeTime();
        
        BigDecimal holdingStock = investPrice.divide(oldStockPrice, MathContext.DECIMAL32);     // 내가 산 주식 개수 
        BigDecimal yieldPrice = holdingStock.multiply(currentPrice); // 수익금 = (투자금/이전종가) * 현재가
        BigDecimal yieldPercent = currentPrice.subtract(oldStockPrice).divide(oldStockPrice, MathContext.DECIMAL32)
                .multiply(new BigDecimal(100));  // (현재가-이전종가)/이전종가 * 100

        BigDecimal salaryYear = new BigDecimal(0);      // 연봉
        BigDecimal salaryMonth = new BigDecimal(0);     // 월급
        
        switch (investDate) {
            case DAY1 -> {}
            case WEEK1 -> {}
            case MONTH1 -> {
                salaryMonth = yieldPrice;
            }
            case MONTH6 -> {
                salaryMonth = yieldPrice.divide(new BigDecimal(6), MathContext.DECIMAL32);
            }
            case YEAR1 -> {
                salaryYear = yieldPrice;
                salaryMonth = salaryYear.divide(new BigDecimal(12), MathContext.DECIMAL32);
            }
            case YEAR5 -> {
                salaryYear = yieldPrice.divide(new BigDecimal(5), MathContext.DECIMAL32);
                salaryMonth = salaryYear.divide(new BigDecimal(12), MathContext.DECIMAL32);
            }
            case YEAR10 -> {
                salaryYear = yieldPrice.divide(new BigDecimal(10), MathContext.DECIMAL32);
                salaryMonth = salaryYear.divide(new BigDecimal(12), MathContext.DECIMAL32);
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + investDate);
        };
        
        // 계산이력 저장
        calcHistRepository.save(
    		CalcHist.builder()
    			.code(code)
    			.company(stockPrice.getCompany())
    			.createdUid(user.getUid())
    			.investDate(oldCloseDate)
    			.investDateName(investDate.getName())
    			.investPrice(investPrice)
    			.yieldPrice(yieldPrice)
    			.yieldPercent(yieldPercent)
    			.price(currentPrice)
    			.sector(stockPrice.getSectorYahoo())
    			.sectorKor(stockPrice.getSectorKor())
    			.build()
		);
        
        return CalculatedRes.builder()
            .code(code)
            .company(stock.getCompany())
            .currentPrice(currentPrice)
            .lastTradingDateTime(lastTradeTime)
            .calculatedValue(
                CalculatedValue.builder()
                    .investPrice(investPrice)
                    .investDate(investDate.getName())
                    .oldPrice(oldStockPrice)
                    .yieldPrice(yieldPrice)
                    .yieldPercent(yieldPercent)
                    .oldCloseDate(oldCloseDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                    .holdingStock(holdingStock)
                    .salaryYear(salaryYear)
                    .salaryMonth(salaryMonth)
                    .build()
            ).build();
        
    }

    /**
     * kospi, 동종업종, 현재가 결과 조회
     * @return
     * @throws Exception
     */

    public CurrentKospiIndustryRes getCurrentKospiIndustry(BuyThenForm buyThenForm) throws Exception {
        CurrentKospiIndustryRes result;

        // 공통
        String code = buyThenForm.getCode(); // 검색한 종목 코드
        Stock stock = stockRepository.findByCode(code)
                .orElseThrow(() -> new Exception("종목코드가 올바르지 않습니다."));

        InvestDate investDate = buyThenForm.getInvestDate(); // 투자 날

        // 믿고싶지 않은 현재가
        RealTimeStock realTimeStock = stockUtils.getStockInfo(code);  // 실시간 주식 정보
        BigDecimal pricePerStock = realTimeStock.getCurrentPrice();   // 실시간 주가
        BigDecimal stocksPerPrice = pricePerStock.divide(buyThenForm.getInvestPrice()). // 보유 주식 환산
                setScale(3, RoundingMode.HALF_EVEN);

        // 코스피
        String kosCode = "KS11"; // 코스피 종목 코드
        Stock kosStock = stockRepository.findByCode(kosCode)
                .orElseThrow(() -> new Exception(
                        "코스피 종목코드(" + kosCode + ")가 올바르지 않습니다.")
                );
        StocksPrice kosStockPrice = stocksPriceRepository.findByStocksId(kosStock.getId())
                .orElseThrow(() -> new Exception(
                        "코스피 종목코드(" + kosCode + ")가 올바르지 않습니다.")
                );

        BigDecimal kosOldPrice; // 코스피 과거 지수
        String oldDate;         // 검색한 과거 날짜
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.");
        switch(investDate) {
            case DAY1 -> {
                kosOldPrice = kosStockPrice.getPrice();
                oldDate = kosStockPrice.getLastTradeDate().format(dateFormatter);
            }
            case WEEK1 -> {
                kosOldPrice = kosStockPrice.getPriceW1();
                oldDate = kosStockPrice.getDateW1().format(dateFormatter);
            }
            case MONTH1 -> {
                kosOldPrice = kosStockPrice.getPriceM1();
                oldDate = kosStockPrice.getDateM1().format(dateFormatter);
            }
            case MONTH6 -> {
                kosOldPrice = kosStockPrice.getPriceM6();
                oldDate = kosStockPrice.getDateM6().format(dateFormatter);
            }
            case YEAR1 -> {
                kosOldPrice = kosStockPrice.getPriceY1();
                oldDate = kosStockPrice.getDateY1().format(dateFormatter);
            }
            case YEAR5 -> {
                kosOldPrice = kosStockPrice.getPriceY5();
                oldDate = kosStockPrice.getDateY5().format(dateFormatter);
            }
            case YEAR10 -> {
                kosOldPrice = kosStockPrice.getPriceY10();
                oldDate = kosStockPrice.getDateY10().format(dateFormatter);
            }
            default -> throw new IllegalArgumentException(
                    "Unexpected value: " + investDate
            );
        }

        RealTimeStock kosRealTimeStock = stockUtils.getStockInfo(kosCode);
        BigDecimal kosCurrentPrice = kosRealTimeStock.getCurrentPrice();    // 코스피 현재 지수
        BigDecimal kosYieldPercent = kosCurrentPrice.subtract(kosOldPrice). // 코스피 상승률
                divide(kosOldPrice, MathContext.DECIMAL32).
                multiply(new BigDecimal(100));

        // 동종업계
        StocksPrice stocksPrice = stocksPriceRepository.findByStocksId(stock.getId())
                .orElseThrow(() -> new Exception("종목 코드가 올바르지 않습니다."));

        String sector = stocksPrice.getSectorYahoo();
        List<StocksPrice> companies = stocksPriceRepository.findBySectorYahoo(sector);

        // Build
        result = CurrentKospiIndustryRes.builder()
                .code(code)
                .company(stock.getCompany())
                .currentValue(
                        CurrentValue.builder()
                                .pricePerStock(pricePerStock)
                                .stockPerPrice(stocksPerPrice)
                                .currentTime(realTimeStock.getCurrentTime())
                                .build())
                .kospiValue(
                        KospiValue.builder()
                        .yieldPercent(kosYieldPercent)
                        .oldDate(oldDate)
                        .oldStock(kosOldPrice)
                        .currentStock(kosCurrentPrice)
                        .currentTime(kosRealTimeStock.getCurrentTime())
                        .build())
                .industryValue(
                        IndustryValue.builder()
                        .name(sector)
                        .yieldPercent(kosCurrentPrice)
                        .companies("test")
                        .companyCnt(2)
                        .build()
                )
                .build();

        return result;
    }
    
    /**
     * 모든 사용자의 계산이력 목록 조회
     * @param PageParam
     * @return
     * @throws Exception
     */
    public CalcHistRes getCalculationHistory(PageParam pageParam) {
        Page<CalcHist> page = calcHistRepository.findAll(PageRequest.of(pageParam.getPage().intValue(),
                pageParam.getSize().intValue(), Sort.by(Direction.DESC, "createdDate")));

        List<CalculationHist> calculationHistList = page.getContent().stream().map(vo -> {
            return CalculationHist.builder().id(vo.getId()).code(vo.getCode()).company(vo.getCompany())
                    .createdUid(vo.getCreatedUid())
                    .createdDate(vo.getCreatedDate().format(DateTimeFormatter.ofPattern(FORMAT)))
                    .investDate(vo.getInvestDate().format(DateTimeFormatter.ofPattern(FORMAT)))
                    .investDateName(vo.getInvestDateName()).investPrice(vo.getInvestPrice()).price(vo.getPrice())
                    .sector(vo.getSector()).sectorKor(vo.getSectorKor()).yieldPrice(vo.getYieldPrice())
                    .yieldPercent(vo.getYieldPercent()).build();
        }).collect(Collectors.toList());

        return CalcHistRes
                .builder()
                .calculationHistList(calculationHistList)
                .pageInfo(
                    PageInfo.builder()
                        .count(page.getTotalElements())
                        .pageNo(page.getNumber())
                        .pageSize(page.getSize())
                        .build()
                )
                .build();
    }
}