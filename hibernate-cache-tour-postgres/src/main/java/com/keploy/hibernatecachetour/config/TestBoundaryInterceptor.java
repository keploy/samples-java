package com.keploy.hibernatecachetour.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Cache;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Reads the keploy.io/test-name header (and its alias X-Keploy-Test-Name)
 * and evicts the entire Hibernate L2 cache when the test name changes.
 * This is what creates the per-test classification boundary that drives
 * issue #3 — StatementCache drift between record and replay caused by
 * differing cache hit/miss patterns.
 *
 * The eviction is intentionally aggressive (cache.evictAllRegions): it
 * forces Hibernate to issue WHERE id = ? and WHERE customer_id = ?
 * against Postgres on the first request after each boundary, so the
 * recorded mocks contain the full pgjdbc bind dance for that path.
 * Without eviction every request after the first would hit the L2
 * cache and never reach pgjdbc, defeating the prepareThreshold flip
 * we want to surface for issue #1.
 */
@Component
public class TestBoundaryInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final EntityManagerFactory emf;
    private volatile String currentTestName = "";

    public TestBoundaryInterceptor(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String name = request.getHeader("keploy.io/test-name");
        if (name == null || name.isEmpty()) {
            name = request.getHeader("X-Keploy-Test-Name");
        }
        if (name == null) {
            return true;
        }
        if (!name.equals(currentTestName)) {
            currentTestName = name;
            SessionFactory sf = emf.unwrap(SessionFactory.class);
            Cache cache = sf.getCache();
            if (cache != null) {
                cache.evictAllRegions();
            }
        }
        return true;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }
}
