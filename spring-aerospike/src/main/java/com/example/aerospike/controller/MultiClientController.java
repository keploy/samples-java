package com.example.aerospike.controller;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.example.aerospike.config.AerospikeProperties;
import com.example.aerospike.config.Policies;
import com.example.aerospike.util.RetryHelper;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;

@RestController
public class MultiClientController {

    private final List<AerospikeClient> clients;
    private final AerospikeProperties props;
    private final WritePolicy wp = Policies.parallelWrite();
    private final Policy rp = Policies.parallelRead();

    public MultiClientController(List<AerospikeClient> multiAerospikeClients,
                                 AerospikeProperties props) {
        this.clients = multiAerospikeClients;
        this.props = props;
    }

    @PostMapping("/multiclient")
    public Map<String, Object> multiClient(@RequestParam(value = "n", defaultValue = "8") int n,
                                           @RequestParam(value = "prefix", defaultValue = "mc") String prefix) {
        if (clients.isEmpty()) {
            throw new IllegalStateException("no multi-clients configured");
        }
        if (n <= 0) n = 8;
        if (n > 128) n = 128;

        ExecutorService pool = Executors.newFixedThreadPool(n);
        Map<String, Object>[] out = new Map[n];
        CountDownLatch done = new CountDownLatch(n);
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            final int idx = i;
            final AerospikeClient c = clients.get(i % clients.size());
            pool.submit(() -> {
                try {
                    String key = prefix + "-" + idx;
                    Key k = new Key(props.getNamespace(), props.getSet(), key);
                    Bin[] bins = { new Bin("idx", idx), new Bin("tag", prefix) };
                    try {
                        RetryHelper.doOp(5, 10, () -> c.put(wp, k, bins));
                    } catch (Exception e) {
                        out[idx] = Map.of("key", key, "error", "put: " + e.getMessage());
                        return;
                    }
                    Record[] recRef = new Record[1];
                    try {
                        RetryHelper.doOp(5, 10, () -> recRef[0] = c.get(rp, k));
                    } catch (Exception e) {
                        out[idx] = Map.of("key", key, "error", "get: " + e.getMessage());
                        return;
                    }
                    out[idx] = recRef[0] == null
                            ? Map.of("key", key, "error", "get: record not found")
                            : Map.of("key", key, "bins", recRef[0].bins);
                } finally {
                    done.countDown();
                }
            });
        }
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdown();
        long durNanos = System.nanoTime() - start;
        return Map.of(
                "workers", n,
                "prefix", prefix,
                "clients", clients.size(),
                "duration", String.format("%.6fms", durNanos / 1_000_000.0),
                "results", Arrays.asList(out)
        );
    }
}
