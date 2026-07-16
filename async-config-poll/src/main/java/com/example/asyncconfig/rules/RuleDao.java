package com.example.asyncconfig.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads rules + their action handlers from MySQL for a (useCase, tenant). This
 * is the primary datastore read the app performs — Keploy captures the MySQL
 * wire traffic as mocks.
 */
@Repository
public class RuleDao {

    private final JdbcTemplate jdbc;

    public RuleDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns a single (use_case, tenant) group, or empty if none match. */
    public List<UseCaseRules> rulesFor(String useCase, String tenant) {
        List<Rule> rules = jdbc.query(
            "SELECT rule_id, constraint_expr AS constraints, rule_type FROM rules "
                + "WHERE use_case = ? AND tenant = ? ORDER BY rule_id",
            (rs, i) -> {
                long ruleId = rs.getLong("rule_id");
                return new Rule(ruleId, rs.getString("constraints"),
                    actionsFor(ruleId), rs.getString("rule_type"));
            },
            useCase, tenant);

        if (rules.isEmpty()) {
            return Collections.emptyList();
        }
        List<UseCaseRules> out = new ArrayList<>(1);
        out.add(new UseCaseRules(useCase, tenant, rules));
        return out;
    }

    private List<RuleAction> actionsFor(long ruleId) {
        return jdbc.query(
            "SELECT basic_action, action_details, seq AS sequence FROM rule_actions "
                + "WHERE rule_id = ? ORDER BY seq",
            (rs, i) -> new RuleAction(rs.getString("basic_action"),
                rs.getString("action_details"), rs.getInt("sequence")),
            ruleId);
    }
}
