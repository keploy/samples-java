package com.example.asyncconfig.config;

import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Talks to a central config service in two distinct ways:
 *
 *  1. BOOT (blocking, one-time): on startup it fetches the required config
 *     buckets and the current app-config version
 *     (GET /v1/buckets/app-config?watch=false). If any of these fails the bean
 *     throws, so the Spring context fails to start — i.e. these are
 *     boot-blocking dependencies. Keploy records them as ordinary (synchronous)
 *     mocks and must serve them for the app to boot on replay.
 *
 *  2. WATCH (background long-poll): after boot, a daemon thread repeatedly
 *     long-polls the SAME endpoint with ?watch=true (carrying the last version)
 *     to pick up config changes. This egress fires from a background thread on
 *     its own schedule — i.e. it is async relative to the ingress testcases —
 *     so Keploy records and replays it through the async-egress engine (lane
 *     "config-watch", matched on watch=true; see keploy.yml).
 */
@Service
public class ConfigWatchService {

    private static final Logger log = LoggerFactory.getLogger(ConfigWatchService.class);

    private final String baseUrl;
    private final long watchIntervalMs;
    private final RestTemplate rt = new RestTemplate();

    private volatile boolean featuresEnabled;
    private volatile int appConfigVersion;
    private volatile boolean watching = true;

    public ConfigWatchService(@Value("${app.config.baseUrl}") String baseUrl,
                              @Value("${app.config.watchIntervalMs:700}") long watchIntervalMs) {
        this.baseUrl = baseUrl;
        this.watchIntervalMs = watchIntervalMs;
    }

    @PostConstruct
    public void init() {
        // (1) Boot-blocking, one-time.
        fetchBucket("app-common");
        Map<String, Object> features = fetchBucket("app-features");
        Map<String, Object> appConfig = fetchBucket("app-config?watch=false"); // get current version
        this.appConfigVersion = intFrom(appConfig, "version", 0);
        Object flag = features == null ? null : features.get("feature.enabled");
        this.featuresEnabled = flag == null || Boolean.parseBoolean(String.valueOf(flag));
        log.info("ConfigWatchService initialized; featuresEnabled={} appConfigVersion={}",
            featuresEnabled, appConfigVersion);

        // (2) Background watch long-poll.
        startWatchPoller();
    }

    private void startWatchPoller() {
        Thread t = new Thread(() -> {
            while (watching) {
                try {
                    Thread.sleep(watchIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    String url = baseUrl + "/v1/buckets/app-config?watch=true&version=" + appConfigVersion;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resp = rt.getForObject(url, Map.class);
                    int v = intFrom(resp, "version", appConfigVersion);
                    if (v > appConfigVersion) {
                        appConfigVersion = v;
                        log.info("config watch: app-config advanced to version {}", v);
                    }
                } catch (Exception e) {
                    // At replay the async engine keep-alives when nothing is
                    // armed; a failed poll is non-fatal to the running app.
                    log.debug("config watch poll failed: {}", e.getMessage());
                }
            }
        }, "config-watch-poller");
        t.setDaemon(true);
        t.start();
    }

    @PreDestroy
    public void stop() {
        watching = false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchBucket(String name) {
        String url = baseUrl + "/v1/buckets/" + name;
        try {
            return rt.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                "ConfigWatchService: failed to fetch config bucket '" + name
                    + "' from " + url + " — application cannot boot", e);
        }
    }

    private static int intFrom(Map<String, Object> m, String key, int dflt) {
        if (m == null || m.get(key) == null) {
            return dflt;
        }
        try {
            return Integer.parseInt(String.valueOf(m.get(key)));
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public boolean isFeaturesEnabled() {
        return featuresEnabled;
    }
}
