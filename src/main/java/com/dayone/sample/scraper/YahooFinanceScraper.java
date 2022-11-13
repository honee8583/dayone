package com.dayone.sample.scraper;

import com.dayone.sample.model.Company;
import com.dayone.sample.model.Dividend;
import com.dayone.sample.model.ScrapedResult;
import com.dayone.sample.model.constants.Month;
import org.hibernate.query.criteria.internal.expression.function.AggregationFunction;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class YahooFinanceScraper implements Scraper{

    private static final String STATISTICS_URL = "https://finance.yahoo.com/quote/%s/history?period1=%d&period2=%d&interval=1mo";
    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s?p=%s";

    private static final long START_TIME = 60 * 60 * 24;

    @Override
    public ScrapedResult scrap(Company company) {
        var scrapResult = new ScrapedResult();
        scrapResult.setCompany(company);

        try {
            long end = System.currentTimeMillis() / 1000;   // currentTimeMillis() 는 1970년 1월 1일 부터 경과한 시간
            String url = String.format(STATISTICS_URL, company.getTicker(), START_TIME, end);
            Connection connection = Jsoup.connect(url);
            Document document = connection.get();   // Execute the request as a GET, and parse the result.

            // data-test 속성이 "historical-prices" 인 태그를 가져온다.
            Elements parsingDivs = document.getElementsByAttributeValue("data-test", "historical-prices");
            Element tableElement = parsingDivs.get(0);  // table 전체

            Element tbody = tableElement.children().get(1);  // thead : 0, tbody: 1, tfoot: 2

            List<Dividend> dividends = new ArrayList<>();
            for (Element e : tbody.children()) {
                String txt = e.text();
                if (!txt.endsWith("Dividend")) {
                    continue;
                }

                String[] splits = txt.split(" ");
                int month = Month.strToNumber(splits[0]);
                int day = Integer.valueOf(splits[1].replace(",", ""));
                int year = Integer.valueOf(splits[2]);
                String dividend = splits[3];

                if (month < 0) {
                    throw new RuntimeException("Unexpected Month enum value -> " + splits[0]);
                }

                dividends.add(Dividend.builder()
                                        .date(LocalDateTime.of(year, month, day, 0, 0))
                                        .dividend(dividend)
                                        .build());
            }
            scrapResult.setDividends(dividends);
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }

        return scrapResult;
    }

    @Override
    public Company scrapCompanyByTicker(String ticker) {
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try {
            Document document = Jsoup.connect(url).get();
            Element titleEle = document.getElementsByTag("h1").get(0);
            String title = titleEle.text().split(" - ")[1].trim();  // 회사명만 추출

            return Company.builder()
                            .ticker(ticker)
                            .name(title)
                            .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
