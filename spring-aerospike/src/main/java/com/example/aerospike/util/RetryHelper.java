package com.example.aerospike.util;

/**
 * App-level retry wrapper for /parallel, /multiclient, /freshclient.
 * Mirrors the Go sample's parallelDo: attempts the operation up to
 * {@code attempts} times with {@code backoffMs} between attempts.
 * The Aerospike client's MaxRetries handles per-op connection retries,
 * but this outer loop gives the pool more time to recycle connections
 * returned by cooperative threads in the same burst.
 */
public final class RetryHelper {
    private RetryHelper() {}

    @FunctionalInterface
    public interface ThrowingOp {
        void run() throws Exception;
    }

    public static void doOp(int attempts, int backoffMs, ThrowingOp op) throws Exception {
        Exception last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                op.run();
                return;
            } catch (Exception e) {
                last = e;
                if (backoffMs > 0) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                }
            }
        }
        throw last;
    }
}
