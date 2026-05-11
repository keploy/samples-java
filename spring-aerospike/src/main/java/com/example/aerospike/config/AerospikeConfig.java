package com.example.aerospike.config;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.Key;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the main Aerospike client used by every controller, plus a small
 * bank of 4 additional clients that {@code /multiclient} round-robins over.
 *
 * <p>Mirrors the Go sample's main.go: same pool sizing, same warmup
 * shape — sequential prelude to walk the proxy past cold-start TLS,
 * then a parallel fill to actually populate the pool so the first
 * {@code /parallel} burst hits warm connections.
 */
@Configuration
public class AerospikeConfig implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AerospikeConfig.class);

    private final AerospikeProperties props;
    private AerospikeClient main;
    private List<AerospikeClient> multi;

    public AerospikeConfig(AerospikeProperties props) {
        this.props = props;
    }

    public ClientPolicy buildClientPolicy() {
        ClientPolicy policy = new ClientPolicy();
        // Pin to seed: single-node CE setups don't have peers worth
        // discovering and the discovery loop just adds noise.
        policy.failIfNotConnected = true;
        policy.connPoolsPerNode = 1;
        policy.maxConnsPerNode = props.getConnectionQueueSize();
        policy.asyncMaxConnsPerNode = props.getConnectionQueueSize();
        // Hold concurrent-open low so a burst doesn't outpace stunnel /
        // the proxy's TLS handshake rate. The Go sample uses the same
        // 16 ceiling for the same reason.
        policy.asyncMaxConnsPerNode = props.getOpeningConnectionThreshold();
        return policy;
    }

    @Bean
    public AerospikeClient aerospikeClient() {
        ClientPolicy policy = buildClientPolicy();
        Host host = new Host(props.getHost(), props.getPort());
        main = new AerospikeClient(policy, host);
        warmup(main, props.getWarmup().getSequential(), props.getWarmup().getParallel());
        return main;
    }

    @Bean
    public List<AerospikeClient> multiAerospikeClients() {
        ClientPolicy policy = buildClientPolicy();
        Host host = new Host(props.getHost(), props.getPort());
        List<AerospikeClient> bank = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            bank.add(new AerospikeClient(policy, host));
        }
        multi = Collections.unmodifiableList(bank);
        return multi;
    }

    /**
     * Issues {@code seq} sequential Exists round-trips followed by
     * {@code par} concurrent ones. The sequential leg walks the proxy
     * past cold-start latency; the parallel leg actually puts {@code par}
     * idle connections into the pool. Without phase 2, only a single
     * connection ever sits in the pool because the Aerospike client
     * returns a used connection to the next op's acquirer.
     */
    private void warmup(AerospikeClient client, int seq, int par) {
        Policy pol = new Policy();
        pol.socketTimeout = 10_000;
        pol.totalTimeout = 30_000;
        pol.maxRetries = 5;
        pol.sleepBetweenRetries = 5;
        try {
            for (int i = 0; i < seq; i++) {
                Key k = new Key(props.getNamespace(), props.getSet(), "warmup-seq-" + i);
                client.exists(pol, k);
            }
            if (par > 0) {
                Thread[] threads = new Thread[par];
                for (int i = 0; i < par; i++) {
                    final int idx = i;
                    threads[i] = new Thread(() -> {
                        try {
                            Key k = new Key(props.getNamespace(), props.getSet(),
                                    "warmup-par-" + idx);
                            client.exists(pol, k);
                        } catch (Throwable t) {
                            // warmup is best-effort; the retry wrappers in /parallel
                            // cover any pool slot that didn't make it.
                        }
                    });
                    threads[i].start();
                }
                for (Thread t : threads) {
                    t.join();
                }
            }
        } catch (Throwable t) {
            log.warn("warmup failed (non-fatal): {}", t.toString());
        }
    }

    @Override
    public void destroy() {
        if (main != null) main.close();
        if (multi != null) multi.forEach(AerospikeClient::close);
    }
}
