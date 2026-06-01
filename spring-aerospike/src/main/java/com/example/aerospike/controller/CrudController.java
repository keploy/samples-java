package com.example.aerospike.controller;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.example.aerospike.config.AerospikeProperties;
import com.example.aerospike.dto.PutRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CrudController {

    private final AerospikeClient client;
    private final AerospikeProperties props;

    public CrudController(AerospikeClient client, AerospikeProperties props) {
        this.client = client;
        this.props = props;
    }

    @PostMapping("/put")
    public Map<String, String> put(@RequestBody PutRequest req) {
        Key k = new Key(props.getNamespace(), props.getSet(), req.getKey());
        Bin[] bins = toBins(req.getBins());
        client.put(null, k, bins);
        return Map.of("status", "ok");
    }

    @GetMapping("/get/{key}")
    public Map<String, Object> get(@PathVariable("key") String key) {
        Key k = new Key(props.getNamespace(), props.getSet(), key);
        Record rec = client.get(null, k);
        if (rec == null) {
            throw new AerospikeException("record not found: " + key);
        }
        return rec.bins;
    }

    @PostMapping("/batch/put")
    public Map<String, Integer> batchPut(@RequestBody List<PutRequest> body) {
        for (PutRequest p : body) {
            Key k = new Key(props.getNamespace(), props.getSet(), p.getKey());
            client.put(null, k, toBins(p.getBins()));
        }
        return Map.of("written", body.size());
    }

    @GetMapping("/batch/get")
    public List<Map<String, Object>> batchGet(@RequestParam("k") List<String> keys) {
        Key[] batch = new Key[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            batch[i] = new Key(props.getNamespace(), props.getSet(), keys.get(i));
        }
        Record[] records = client.get(null, batch);
        List<Map<String, Object>> out = new ArrayList<>(records.length);
        for (Record r : records) {
            out.add(r == null ? null : r.bins);
        }
        return out;
    }

    private static Bin[] toBins(Map<String, Object> bins) {
        if (bins == null) return new Bin[0];
        Bin[] out = new Bin[bins.size()];
        int i = 0;
        for (Map.Entry<String, Object> e : bins.entrySet()) {
            out[i++] = toBin(e.getKey(), e.getValue());
        }
        return out;
    }

    /**
     * Coerce JSON-decoded values into Aerospike Bin values. The
     * Aerospike Java client's Bin constructor is overloaded but
     * doesn't accept {@code Number} — pick the concrete numeric type
     * Jackson handed us (Integer / Long / Double) and unwrap.
     */
    static Bin toBin(String name, Object value) {
        if (value instanceof Integer) return new Bin(name, (Integer) value);
        if (value instanceof Long) return new Bin(name, (Long) value);
        if (value instanceof Double) return new Bin(name, (Double) value);
        if (value instanceof Float) return new Bin(name, (Float) value);
        if (value instanceof Boolean) return new Bin(name, ((Boolean) value) ? 1 : 0);
        if (value instanceof List) return new Bin(name, (List<?>) value);
        if (value instanceof Map) return new Bin(name, (Map<?, ?>) value);
        return new Bin(name, value == null ? null : value.toString());
    }
}
