package com.dayone.sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class ScrapedResult {

    private Company company;
    private List<Dividend> dividends;

    public ScrapedResult() {
        this.dividends = new ArrayList<>();
    }
}
