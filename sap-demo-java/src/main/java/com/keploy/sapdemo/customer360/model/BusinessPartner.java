package com.keploy.sapdemo.customer360.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps a subset of SAP's {@code A_BusinessPartner} entity.
 *
 * <p>SAP returns ~80 fields on this entity; we project only the ones this
 * service consumes. {@link JsonIgnoreProperties} is intentionally set to
 * {@code ignoreUnknown = true} so a downstream addition of a new field by
 * SAP doesn't break this client — but a <em>removal</em> or <em>rename</em>
 * of a consumed field will surface as a missing value during
 * {@code keploy test}, which is exactly the contract-drift signal teams
 * need after quarterly S/4HANA updates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessPartner {

    @JsonProperty("BusinessPartner")
    private String businessPartner;

    @JsonProperty("BusinessPartnerCategory")
    private String category;

    @JsonProperty("BusinessPartnerFullName")
    private String fullName;

    @JsonProperty("BusinessPartnerGrouping")
    private String grouping;

    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("LastName")
    private String lastName;

    @JsonProperty("OrganizationBPName1")
    private String organizationName;

    @JsonProperty("CreatedByUser")
    private String createdBy;

    @JsonProperty("CreationDate")
    private String createdDate;

    @JsonProperty("LastChangedByUser")
    private String lastChangedBy;

    @JsonProperty("LastChangeDate")
    private String lastChangeDate;

    @JsonProperty("BusinessPartnerIsBlocked")
    private Boolean blocked;

    @JsonProperty("ETag")
    private String etag;

    // ---- accessors ---------------------------------------------------------

    public String getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(String v) { this.businessPartner = v; }

    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }

    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }

    public String getGrouping() { return grouping; }
    public void setGrouping(String v) { this.grouping = v; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { this.firstName = v; }

    public String getLastName() { return lastName; }
    public void setLastName(String v) { this.lastName = v; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String v) { this.organizationName = v; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String v) { this.createdDate = v; }

    public String getLastChangedBy() { return lastChangedBy; }
    public void setLastChangedBy(String v) { this.lastChangedBy = v; }

    public String getLastChangeDate() { return lastChangeDate; }
    public void setLastChangeDate(String v) { this.lastChangeDate = v; }

    public Boolean getBlocked() { return blocked; }
    public void setBlocked(Boolean v) { this.blocked = v; }

    public String getEtag() { return etag; }
    public void setEtag(String v) { this.etag = v; }
}
