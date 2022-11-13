package com.dayone.sample.service;

import com.dayone.sample.exception.impl.NoCompanyException;
import com.dayone.sample.model.Company;
import com.dayone.sample.model.Dividend;
import com.dayone.sample.model.ScrapedResult;
import com.dayone.sample.model.constants.CacheKey;
import com.dayone.sample.persist.CompanyRepository;
import com.dayone.sample.persist.DividendRepository;
import com.dayone.sample.persist.entity.CompanyEntity;
import com.dayone.sample.persist.entity.DividendEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;


    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName) {
        log.info("search Company -> " + companyName);

        // 1. 회사명을 기준으로 회사 정보를 조회
        CompanyEntity company
                = this.companyRepository.findByName(companyName)
                .orElseThrow(() -> new NoCompanyException());

        // 2. 조회된 회사 아이디로 배당금 정보 조회
        List<DividendEntity> dividendEntities
                = this.dividendRepository.findAllByCompanyId(company.getId());

        // 3. 결과 조합 후 반환
        /*
        List<Dividend> dividends = new ArrayList<>();
        for (var entity: dividendEntities) {
            dividends.add(Dividend.builder()
                            .date(entity.getDate())
                            .dividend(entity.getDividend())
                            .build());
        }*/

        List<Dividend> dividendList = dividendEntities.stream()
                .map(entity -> Dividend.builder()
                                    .date(entity.getDate())
                                    .dividend(entity.getDividend())
                                    .build()).collect(Collectors.toList());

        return new ScrapedResult(Company.builder()
                                        .ticker(company.getTicker())
                                        .name(company.getName()).build(),
                                        dividendList);
    }
}
