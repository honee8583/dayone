package com.dayone.sample.scheduler;

import com.dayone.sample.model.Company;
import com.dayone.sample.model.ScrapedResult;
import com.dayone.sample.model.constants.CacheKey;
import com.dayone.sample.persist.CompanyRepository;
import com.dayone.sample.persist.DividendRepository;
import com.dayone.sample.persist.entity.CompanyEntity;
import com.dayone.sample.persist.entity.DividendEntity;
import com.dayone.sample.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableCaching
@AllArgsConstructor
public class ScraperScheduler {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    private final Scraper yahooFinanceScraper;

    // Scheduler가 동작할때마다 캐시의 데이터를 삭제
    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true)   // Redis 캐시의 finance에 해당하는 데이터는 모두 비운다.
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    public void yahooFinanceScheduling() {
        log.info("scraping scheduler is started");

        // 저장된 회사 목록을 조회
        List<CompanyEntity> companies = this.companyRepository.findAll();

        // 회사마다 배당금 정보를 새로 스크래핑
        for (var company : companies) {
            log.info("scraping scheduler is started -> " + company.getName());
            ScrapedResult scrapedResult
                    = this.yahooFinanceScraper.scrap(Company.builder()
                                    .name(company.getName())
                                    .ticker(company.getTicker())
                                    .build());
            // 스크래핑한 배당금 정보 중 데이터베이스에 없는 값은 저장
            scrapedResult.getDividends().stream()
                    // Dividend -> DividendEntity
                    .map(e -> new DividendEntity(company.getId(), e))
                    // DividendEntity 중복 체크후 저장
                    .forEach(e -> {
                        boolean exists
                                = this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                        if (!exists) {
                            this.dividendRepository.save(e);
                            log.info("insert new Dividend -> " + e);
                        }
                    });

            // 연속적으로 스크래핑 대상 사이트 서버에 요청을 날리지 않도록 일시정지
            try {
                Thread.sleep(3000); // 3seconds
            } catch (InterruptedException e) {
                // 인터럽트를 받은 스레드가 blocking 될 수 있는 메소드를 실행했을 때 발생
                Thread.currentThread().interrupt();
            }
        }
    }

    /*
    @Scheduled(cron = "0/5 * * * * *")
    public void test() {
        System.out.println("now -> "  + Thread.currentThread().getName() + " : " + System.currentTimeMillis());
    }*/
}
