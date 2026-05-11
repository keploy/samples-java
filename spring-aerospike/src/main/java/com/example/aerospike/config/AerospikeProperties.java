package com.example.aerospike.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aerospike")
public class AerospikeProperties {
    private String host = "127.0.0.1";
    private int port = 3000;
    private String namespace = "test";
    private String set = "demo";
    private int connectionQueueSize = 256;
    private int openingConnectionThreshold = 16;
    private Warmup warmup = new Warmup();

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getSet() { return set; }
    public void setSet(String set) { this.set = set; }
    public int getConnectionQueueSize() { return connectionQueueSize; }
    public void setConnectionQueueSize(int v) { this.connectionQueueSize = v; }
    public int getOpeningConnectionThreshold() { return openingConnectionThreshold; }
    public void setOpeningConnectionThreshold(int v) { this.openingConnectionThreshold = v; }
    public Warmup getWarmup() { return warmup; }
    public void setWarmup(Warmup warmup) { this.warmup = warmup; }

    public static class Warmup {
        private int sequential = 8;
        private int parallel = 32;
        public int getSequential() { return sequential; }
        public void setSequential(int v) { this.sequential = v; }
        public int getParallel() { return parallel; }
        public void setParallel(int v) { this.parallel = v; }
    }
}
