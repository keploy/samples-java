package io.keploy.samples.dropwizarddedup.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderRequest {
    private String customer;
    private String sku;
    private int quantity;
    private boolean priority;
    private String status;

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @JsonProperty("priority")
    public boolean isPriority() {
        return priority;
    }

    @JsonProperty("priority")
    public void setPriority(boolean priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status == null ? "packed" : status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
