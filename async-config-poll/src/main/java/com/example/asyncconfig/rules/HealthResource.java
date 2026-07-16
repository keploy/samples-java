package com.example.asyncconfig.rules;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * /health — a small, deterministic health payload that also runs a real MySQL
 * SELECT so the datastore is exercised (and mocked) on this endpoint too. It is
 * served as a Jersey resource rather than via live actuator so the body is
 * stable across record/replay (actuator's diskSpace.free changes between runs
 * and would break replay matching).
 */
@Component
@Path("/health")
public class HealthResource {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    @Autowired
    public HealthResource(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    public Response health() throws Exception {
        Integer hello = jdbc.queryForObject("SELECT 1", Integer.class); // exercises MySQL

        Map<String, Object> db = new LinkedHashMap<>();
        db.put("status", "UP");
        db.put("database", "MySQL");
        db.put("hello", hello);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("db", db);

        return Response.ok(mapper.writeValueAsString(body)).build();
    }
}
