package com.example.tidbstmtcache;

import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pulsar client + producer beans wired for the partitioned-topic
 * round-robin scenario that triggers the keploy replay regression.
 *
 * Routing policy: MessageRoutingMode.RoundRobinPartition is the Java
 * client default. The starting partition cursor is randomised per
 * producer instance, so the partition the very first SEND in a session
 * lands on is non-deterministic across runs. Sequential SENDs from
 * the same producer then walk through partitions in order. Net effect:
 * a recording captured against partition-N will, at replay time, see
 * the live SEND target partition-(N+k mod P) for some k chosen at
 * producer construction.
 *
 * Why we do NOT pin a single partition (e.g. SinglePartition + fixed
 * messageKey): pinning would mask the bug, which is the whole reason
 * this sample exists. The point is to reproduce the routing mismatch
 * end-to-end so the matcher loosening in keploy/enterprise (baseTopic
 * in pulsar/replayer/replayer.go) can be verified against a real Java
 * producer.
 *
 * Why we accept a topic that may not yet exist on broker startup:
 * docker-compose's pulsar-init container runs `pulsar-admin topics
 * create-partitioned-topic` after the broker is healthy but before
 * this app's healthcheck flips green. PulsarClient lazy-resolves the
 * partitioned metadata on first SEND, so as long as the topic exists
 * before /events/patch is hit, no extra synchronisation is needed.
 */
@Configuration
public class PulsarConfig {

    @Value("${pulsar.broker-url}")
    private String brokerUrl;

    @Value("${pulsar.topic}")
    private String topic;

    @Bean(destroyMethod = "close")
    public PulsarClient pulsarClient() throws PulsarClientException {
        return PulsarClient.builder()
                .serviceUrl(brokerUrl)
                .build();
    }

    @Bean(destroyMethod = "close")
    public Producer<byte[]> eventProducer(PulsarClient client) throws PulsarClientException {
        return client.newProducer(Schema.BYTES)
                .topic(topic)
                .messageRoutingMode(MessageRoutingMode.RoundRobinPartition)
                .blockIfQueueFull(true)
                .create();
    }
}
