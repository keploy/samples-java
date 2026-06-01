package com.example.aerospike.controller;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.cluster.Node;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    private final AerospikeClient client;

    public HealthController(AerospikeClient client) {
        this.client = client;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Node[] nodes = client.getNodes();
        return Map.of(
                "nodes", nodes.length,
                "namespaces", "test"
        );
    }
}
