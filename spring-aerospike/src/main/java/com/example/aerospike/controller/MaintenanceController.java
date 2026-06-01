package com.example.aerospike.controller;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Key;
import com.example.aerospike.config.AerospikeProperties;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class MaintenanceController {

    private final AerospikeClient client;
    private final AerospikeProperties props;

    public MaintenanceController(AerospikeClient client, AerospikeProperties props) {
        this.client = client;
        this.props = props;
    }

    @PostMapping("/touch/{key}")
    public Map<String, String> touch(@PathVariable("key") String key) {
        Key k = new Key(props.getNamespace(), props.getSet(), key);
        client.touch(null, k);
        return Map.of("status", "touched");
    }

    @DeleteMapping("/key/{key}")
    public Map<String, Boolean> delete(@PathVariable("key") String key) {
        Key k = new Key(props.getNamespace(), props.getSet(), key);
        boolean deleted = client.delete(null, k);
        return Map.of("deleted", deleted);
    }
}
