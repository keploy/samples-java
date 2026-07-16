package com.example.asyncconfig.rules;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rule-engine endpoint: GET /rules/{useCase}. Requires the X-Tenant-Id and
 * X-Agent-Id headers and returns the ordered rules for the (useCase, tenant),
 * read from MySQL:
 * [{"use_case","tenant","rules":[{"rule_id","constraints","actions":[...],"rule_type"}]}]
 */
@Component
@Path("/rules")
public class RulesResource {

    private final RuleDao ruleDao;
    private final ObjectMapper mapper;
    private final long delayMs;

    @Autowired
    public RulesResource(RuleDao ruleDao, ObjectMapper mapper,
                         @Value("${app.rules.delayMs:0}") long delayMs) {
        this.ruleDao = ruleDao;
        this.mapper = mapper;
        this.delayMs = delayMs;
    }

    @GET
    @Path("/{useCase}")
    @Produces("application/json;charset=utf-8")
    public Response rules(@PathParam("useCase") String useCase,
                          @HeaderParam("X-Tenant-Id") String tenantId,
                          @HeaderParam("X-Agent-Id") String agentId) throws Exception {
        if (isBlank(tenantId) || isBlank(agentId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .type("application/json")
                .entity("{\"error\":\"X-Tenant-Id and X-Agent-Id headers are required\"}")
                .build();
        }
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs); // widen the request window for the fast-poller demo
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        List<UseCaseRules> result = ruleDao.rulesFor(useCase, tenantId);
        String json = mapper.writeValueAsString(result);
        return Response.ok(json).build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
