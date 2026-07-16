package com.example.asyncconfig.rules;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A single rule: constraint expression + ordered action handlers. */
public class Rule {
    @JsonProperty("rule_id")
    private long ruleId;
    private String constraints;
    private List<RuleAction> actions;
    @JsonProperty("rule_type")
    private String ruleType;

    public Rule(long ruleId, String constraints, List<RuleAction> actions, String ruleType) {
        this.ruleId = ruleId;
        this.constraints = constraints;
        this.actions = actions;
        this.ruleType = ruleType;
    }

    public long getRuleId() { return ruleId; }
    public String getConstraints() { return constraints; }
    public List<RuleAction> getActions() { return actions; }
    public String getRuleType() { return ruleType; }
}
