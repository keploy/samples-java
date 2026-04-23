package com.keploy.sapdemo.customer360.model;

import com.keploy.sapdemo.customer360.persistence.CustomerNote;
import com.keploy.sapdemo.customer360.persistence.CustomerTag;

import java.time.Instant;
import java.util.List;

/**
 * The aggregated "Customer 360" response returned to downstream consumers.
 *
 * <p>Produced by
 * {@link com.keploy.sapdemo.customer360.service.Customer360AggregatorService}
 * from <b>three parallel SAP OData calls + two parallel Postgres queries +
 * one audit INSERT</b>. This composite shape is the kind of payload
 * downstream CRM / portal / analytics pipelines consume in typical RISE
 * with SAP BTP landscapes.
 */
public class Customer360View {

    private String customerId;
    private BusinessPartner partner;
    private List<BusinessPartnerAddress> addresses;
    private List<BusinessPartnerRole> roles;
    private List<CustomerTag> tags;
    private List<CustomerNote> notes;
    private Instant aggregatedAt;
    private String correlationId;
    private String dataSource;
    private Integer elapsedMs;

    public Customer360View() {
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }

    public BusinessPartner getPartner() { return partner; }
    public void setPartner(BusinessPartner v) { this.partner = v; }

    public List<BusinessPartnerAddress> getAddresses() { return addresses; }
    public void setAddresses(List<BusinessPartnerAddress> v) { this.addresses = v; }

    public List<BusinessPartnerRole> getRoles() { return roles; }
    public void setRoles(List<BusinessPartnerRole> v) { this.roles = v; }

    public List<CustomerTag> getTags() { return tags; }
    public void setTags(List<CustomerTag> v) { this.tags = v; }

    public List<CustomerNote> getNotes() { return notes; }
    public void setNotes(List<CustomerNote> v) { this.notes = v; }

    public Integer getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Integer v) { this.elapsedMs = v; }

    public Instant getAggregatedAt() { return aggregatedAt; }
    public void setAggregatedAt(Instant v) { this.aggregatedAt = v; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String v) { this.correlationId = v; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String v) { this.dataSource = v; }
}
