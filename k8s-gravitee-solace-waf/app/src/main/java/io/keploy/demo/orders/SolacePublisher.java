package io.keploy.demo.orders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solacesystems.jcsmp.*;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Native Solace SMF v3 (TCP 55555) publisher AND consumer using Solace JCSMP:
 *   - Publishes OrderCreated events as DeliveryMode.DIRECT JSON TextMessages to orders/created
 *   - Subscribes via XMLMessageConsumer to orders/cancelled and consumes broker-pushed SMF
 *     OrderCancelled deliveries in-band (recording Kind: Solace publish, subscribe, and consume mocks).
 */
@Component
class SolacePublisher {
    private static final Logger log = LoggerFactory.getLogger(SolacePublisher.class);

    private final ObjectMapper mapper;
    private final String host;
    private final String vpn;
    private final String username;
    private final String password;
    private final String cancelTopic;

    private final Map<String, CompletableFuture<String>> pendingCancels = new ConcurrentHashMap<>();

    private JCSMPSession session;
    private XMLMessageProducer producer;
    private XMLMessageConsumer consumer;

    SolacePublisher(
            ObjectMapper mapper,
            @Value("${solace.smf.host:${SOLACE_SMF_HOST:tcp://solace.messaging.svc.cluster.local:55555}}") String host,
            @Value("${solace.smf.vpn:${SOLACE_SMF_VPN:default}}") String vpn,
            @Value("${solace.smf.username:${SOLACE_SMF_USERNAME:default}}") String username,
            @Value("${solace.smf.password:${SOLACE_SMF_PASSWORD:}}") String password,
            @Value("${orders.cancel-topic:orders/cancelled}") String cancelTopic) {
        this.mapper = mapper;
        this.host = host;
        this.vpn = vpn;
        this.username = username;
        this.password = password;
        this.cancelTopic = cancelTopic;
    }

    private synchronized void ensureSession() throws JCSMPException {
        if (session != null && !session.isClosed() && producer != null && !producer.isClosed()) {
            return;
        }
        closeQuietly();
        JCSMPProperties props = new JCSMPProperties();
        props.setProperty(JCSMPProperties.HOST, host);
        props.setProperty(JCSMPProperties.VPN_NAME, vpn);
        props.setProperty(JCSMPProperties.USERNAME, username);
        props.setProperty(JCSMPProperties.PASSWORD, password);
        props.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, false);

        JCSMPChannelProperties cp = (JCSMPChannelProperties) props.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES);
        cp.setConnectTimeoutInMillis(3000);
        cp.setReadTimeoutInMillis(3000);
        cp.setConnectRetries(0);
        cp.setReconnectRetries(0);
        cp.setKeepAliveIntervalInMillis(3000);

        session = JCSMPFactory.onlyInstance().createSession(props, null, eventArgs ->
                log.warn("Solace session event: {} ({})", eventArgs.getEvent(), eventArgs.getInfo()));
        session.connect();
        producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
            @Override
            public void responseReceivedEx(Object key) {}

            @Override
            public void handleErrorEx(Object key, JCSMPException cause, long timestamp) {
                log.error("Solace publish error for key={}: {}", key, cause.getMessage());
            }
        });
        consumer = session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                handleIncomingCancel(msg);
            }

            @Override
            public void onException(JCSMPException e) {
                log.warn("Solace consumer exception: {}", e.getMessage());
            }
        });
        consumer.start();
        log.info("connected to Solace SMF at {} (vpn={})", host, vpn);
    }

    private void handleIncomingCancel(BytesXMLMessage msg) {
        try {
            String text;
            if (msg instanceof TextMessage tm) {
                text = tm.getText();
            } else {
                byte[] b = msg.getBytes();
                text = b == null ? "" : new String(b, StandardCharsets.UTF_8);
            }
            String dest = msg.getDestination() != null ? msg.getDestination().getName() : cancelTopic;
            JsonNode node = mapper.readTree(text);
            String orderId = node.hasNonNull("orderId") ? node.get("orderId").asText() : "";
            log.info("consumed SMF message from topic {} for orderId={}", dest, orderId);
            if (!orderId.isEmpty()) {
                pendingCancels.computeIfAbsent(orderId, k -> new CompletableFuture<>()).complete(text);
            }
        } catch (Exception e) {
            log.warn("failed to handle incoming SMF cancel message: {}", e.toString());
        }
    }

    /** Publishes a DIRECT TextMessage with JSON payload and returns 200 on success or 502 on error. */
    synchronized int publish(String topicName, Map<String, Object> event, String correlationId) throws Exception {
        String body = mapper.writeValueAsString(event);
        try {
            ensureSession();
            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setDeliveryMode(DeliveryMode.DIRECT);
            msg.setText(body);
            msg.setHTTPContentType("application/json");
            if (correlationId != null && !correlationId.isBlank()) {
                msg.setCorrelationId(correlationId);
            }
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
            producer.send(msg, topic);
            log.info("published {} via SMF DIRECT to topic {} (corrId={})", event.get("eventType"), topicName, correlationId);
            return 200;
        } catch (JCSMPException e) {
            log.error("SMF publish failed for topic {}: {}", topicName, e.getMessage());
            closeQuietly();
            return 502;
        }
    }

    /**
     * Subscribes to orders/cancelled/<orderId> via native SMF, publishes OrderCancelled on that topic,
     * awaits the broker-pushed SMF consume delivery on this session's XMLMessageConsumer, and unsubscribes.
     */
    String publishAndConsumeCancel(String orderId, String reason, String correlationId) throws Exception {
        String perOrderTopicName = cancelTopic + "/" + orderId;
        Topic perOrderTopic = JCSMPFactory.onlyInstance().createTopic(perOrderTopicName);
        CompletableFuture<String> fut = pendingCancels.computeIfAbsent(orderId, k -> new CompletableFuture<>());
        boolean subscribed = false;
        try {
            synchronized (this) {
                ensureSession();
                session.addSubscription(perOrderTopic);
                subscribed = true;
                log.info("subscribed via SMF to topic {}", perOrderTopicName);
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("eventType", "OrderCancelled");
                event.put("orderId", orderId);
                event.put("reason", reason);
                String body = mapper.writeValueAsString(event);

                TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                msg.setDeliveryMode(DeliveryMode.DIRECT);
                msg.setText(body);
                msg.setHTTPContentType("application/json");
                if (correlationId != null && !correlationId.isBlank()) {
                    msg.setCorrelationId(correlationId);
                }
                producer.send(msg, perOrderTopic);
                log.info("published OrderCancelled via SMF DIRECT to {} for {}", perOrderTopicName, orderId);
            }
            fut.get(4, TimeUnit.SECONDS);
            return "solace-smf";
        } finally {
            pendingCancels.remove(orderId);
            if (subscribed) {
                synchronized (this) {
                    if (session != null && !session.isClosed()) {
                        try {
                            session.removeSubscription(perOrderTopic);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    @PreDestroy
    synchronized void closeQuietly() {
        if (consumer != null) {
            try { consumer.close(); } catch (Exception ignored) {}
            consumer = null;
        }
        if (producer != null) {
            try { producer.close(); } catch (Exception ignored) {}
            producer = null;
        }
        if (session != null) {
            try { session.closeSession(); } catch (Exception ignored) {}
            session = null;
        }
    }
}
