package com.dayone.sample.scraper;

import com.dayone.sample.model.Company;
import com.dayone.sample.model.ScrapedResult;

public interface Scraper {
    Company scrapCompanyByTicker(String ticker);
    ScrapedResult scrap(Company company);
}
