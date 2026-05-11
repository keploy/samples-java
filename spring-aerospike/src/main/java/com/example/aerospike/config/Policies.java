package com.example.aerospike.config;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;

/**
 * Per-op policy helpers used by /parallel, /multiclient, /freshclient
 * to ride out the cold-pool burst the Go sample's parallelWrite/Read
 * policies were tuned for. Generous timeouts + retries + sleep gives
 * the pool time to recycle connections across cooperative goroutine-
 * equivalents (threads).
 */
public final class Policies {
    private Policies() {}

    public static WritePolicy parallelWrite() {
        WritePolicy p = new WritePolicy();
        p.socketTimeout = 10_000;
        p.totalTimeout = 30_000;
        p.maxRetries = 10;
        p.sleepBetweenRetries = 5;
        return p;
    }

    public static Policy parallelRead() {
        Policy p = new Policy();
        p.socketTimeout = 10_000;
        p.totalTimeout = 30_000;
        p.maxRetries = 10;
        p.sleepBetweenRetries = 5;
        return p;
    }
}
