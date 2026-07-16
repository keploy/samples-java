package com.example.asyncconfig.rules;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One (use_case, tenant) group with its ordered rules. */
public class UseCaseRules {
    @JsonProperty("use_case")
    private String useCase;
    private String tenant;
    private List<Rule> rules;

    public UseCaseRules(String useCase, String tenant, List<Rule> rules) {
        this.useCase = useCase;
        this.tenant = tenant;
        this.rules = rules;
    }

    public String getUseCase() { return useCase; }
    public String getTenant() { return tenant; }
    public List<Rule> getRules() { return rules; }
}
