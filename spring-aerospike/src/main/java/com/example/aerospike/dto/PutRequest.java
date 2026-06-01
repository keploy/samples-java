package com.example.aerospike.dto;

import java.util.Map;

public class PutRequest {
    private String key;
    private Map<String, Object> bins;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public Map<String, Object> getBins() { return bins; }
    public void setBins(Map<String, Object> bins) { this.bins = bins; }
}
