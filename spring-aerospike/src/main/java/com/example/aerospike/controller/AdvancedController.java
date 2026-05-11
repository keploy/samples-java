package com.example.aerospike.controller;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.ListOperation;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.example.aerospike.config.AerospikeProperties;
import com.example.aerospike.dto.PutRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * The four extra endpoints that the Go sample carries — /scan,
 * /query, /udf, /cdt/list/append, /cdt/map/put. The scripts don't
 * exercise these, but ports of the Go sample include them for parity.
 */
@RestController
public class AdvancedController {

    private final AerospikeClient client;
    private final AerospikeProperties props;

    public AdvancedController(AerospikeClient client, AerospikeProperties props) {
        this.client = client;
        this.props = props;
    }

    @PostMapping("/scan")
    public Map<String, Integer> scan() {
        int[] count = {0};
        client.scanAll(null, props.getNamespace(), props.getSet(), (key, record) -> count[0]++);
        return Map.of("scanned", count[0]);
    }

    @PostMapping("/query")
    public Map<String, Integer> query() {
        Statement stmt = new Statement();
        stmt.setNamespace(props.getNamespace());
        stmt.setSetName(props.getSet());
        stmt.setFilter(Filter.range("age", 0, 99));
        int count = 0;
        try (RecordSet rs = client.query(null, stmt)) {
            while (rs.next()) {
                count++;
            }
        }
        return Map.of("matched", count);
    }

    @PostMapping("/udf")
    public Map<String, Object> udf(@RequestBody PutRequest req) {
        Key k = new Key(props.getNamespace(), props.getSet(), req.getKey());
        Object out = client.execute(null, k, "transform", "apply", Value.get("bin"), Value.get(1));
        return Map.of("result", out == null ? "null" : out.toString());
    }

    @PostMapping("/cdt/list/append")
    public Map<String, String> cdtListAppend(@RequestBody PutRequest req) {
        Key k = new Key(props.getNamespace(), props.getSet(), req.getKey());
        Object v = req.getBins() == null ? null : req.getBins().get("value");
        client.operate(null, k, ListOperation.append("items", Value.get(v)));
        return Map.of("status", "appended");
    }

    @PostMapping("/cdt/map/put")
    public Map<String, String> cdtMapPut(@RequestBody PutRequest req) {
        Key k = new Key(props.getNamespace(), props.getSet(), req.getKey());
        Map<String, Object> bins = req.getBins() == null ? new HashMap<>() : req.getBins();
        Value mapKey = Value.get(bins.get("mapKey"));
        Value mapVal = Value.get(bins.get("mapVal"));
        client.operate(null, k, MapOperation.put(MapPolicy.Default, "mapBin", mapKey, mapVal));
        return Map.of("status", "put");
    }
}
