package io.keploy.demo.orders;

import jakarta.annotation.PreDestroy;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

/**
 * Plain RESP2 Redis client (Jedis) for POST idempotency (SET NX EX) and GET read-through caching.
 */
@Component
class RedisCacheClient {
    private final JedisPool pool;

    RedisCacheClient(@Value("${redis.host}") String host, @Value("${redis.port}") int port) {
        GenericObjectPoolConfig<Jedis> cfg = new GenericObjectPoolConfig<>();
        cfg.setMaxTotal(8);
        cfg.setMaxIdle(4);
        cfg.setMinIdle(0);
        cfg.setTestOnBorrow(false);
        cfg.setTestWhileIdle(false);
        this.pool = new JedisPool(cfg, host, port, 3000);
    }

    /** Returns true if the idempotency key was freshly claimed, false if already present (duplicate). */
    boolean claimIdempotency(String customerId, String reference, String orderId) {
        String key = "idempotency:" + customerId + ":" + reference;
        try (Jedis j = pool.getResource()) {
            String res = j.set(key, orderId, SetParams.setParams().nx().ex(3600));
            return "OK".equals(res);
        }
    }

    void releaseIdempotency(String customerId, String reference) {
        String key = "idempotency:" + customerId + ":" + reference;
        try (Jedis j = pool.getResource()) {
            j.del(key);
        }
    }

    String getOrder(String orderId) {
        try (Jedis j = pool.getResource()) {
            return j.get("order:" + orderId);
        }
    }

    void putOrder(String orderId, String json) {
        try (Jedis j = pool.getResource()) {
            j.setex("order:" + orderId, 300, json);
        }
    }

    void evictOrder(String orderId) {
        try (Jedis j = pool.getResource()) {
            j.del("order:" + orderId);
        }
    }

    @PreDestroy
    void close() {
        pool.close();
    }
}
