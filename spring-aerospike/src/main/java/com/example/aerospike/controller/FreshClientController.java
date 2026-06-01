package com.example.aerospike.controller;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Host;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.example.aerospike.config.AerospikeConfig;
import com.example.aerospike.config.AerospikeProperties;
import com.example.aerospike.config.Policies;
import com.example.aerospike.util.RetryHelper;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;

@RestController
public class FreshClientController {

    private final AerospikeConfig config;
    private final AerospikeProperties props;
    private final WritePolicy wp = Policies.parallelWrite();
    private final Policy rp = Policies.parallelRead();
    private final Semaphore concurrencyCap = new Semaphore(4);

    public FreshClientController(AerospikeConfig config, AerospikeProperties props) {
        this.config = config;
        this.props = props;
    }

    @PostMapping("/freshclient")
    public Map<String, Object> freshClient(@RequestParam(value = "n", defaultValue = "4") int n,
                                           @RequestParam(value = "prefix", defaultValue = "fc") String prefix) {
        if (n <= 0) n = 4;
        if (n > 16) n = 16;

        ClientPolicy cp = config.buildClientPolicy();
        Host host = new Host(props.getHost(), props.getPort());

        ExecutorService pool = Executors.newFixedThreadPool(n);
        Map<String, Object>[] out = new Map[n];
        CountDownLatch done = new CountDownLatch(n);
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    concurrencyCap.acquire();
                    try {
                        String key = prefix + "-" + idx;
                        try (AerospikeClient c = new AerospikeClient(cp, host)) {
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
                        } catch (Exception e) {
                            out[idx] = Map.of("key", key, "error", "newclient: " + e.getMessage());
                        }
                    } finally {
                        concurrencyCap.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
                "concurrency", 4,
                "duration", String.format("%.6fms", durNanos / 1_000_000.0),
                "results", Arrays.asList(out)
        );
    }
}
