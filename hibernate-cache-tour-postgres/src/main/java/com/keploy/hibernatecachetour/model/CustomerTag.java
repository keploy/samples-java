package com.keploy.hibernatecachetour.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Tag entity. customer_id and priority are both indexed columns the
 * exerciser hits on every iteration, which keeps the prepareThreshold
 * counter climbing for those two SQL handles independently of the
 * /customer/{id} endpoint.
 */
@Entity
@Table(name = "customer_tag")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CustomerTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_id")
    private Integer customerId;

    private String tag;
    private Integer priority;

    public CustomerTag() {}

    public CustomerTag(Integer customerId, String tag, Integer priority) {
        this.customerId = customerId;
        this.tag = tag;
        this.priority = priority;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
