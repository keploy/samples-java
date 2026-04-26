package io.keploy.samples.javadedup.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Product {

    @JsonProperty("product_id")
    private String productId;
    private String name;
    private String description;
    private List<String> tags;

    public Product() {
    }

    public Product(String productId, String name, String description, List<String> tags) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.tags = tags;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
