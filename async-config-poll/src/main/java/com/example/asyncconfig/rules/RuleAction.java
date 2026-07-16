package com.example.asyncconfig.rules;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One action handler within a rule (handler class + details + order). */
public class RuleAction {
    @JsonProperty("basic_action")
    private String basicAction;
    @JsonProperty("action_details")
    private String actionDetails;
    private int sequence;

    public RuleAction(String basicAction, String actionDetails, int sequence) {
        this.basicAction = basicAction;
        this.actionDetails = actionDetails;
        this.sequence = sequence;
    }

    public String getBasicAction() { return basicAction; }
    public String getActionDetails() { return actionDetails; }
    public int getSequence() { return sequence; }
}
