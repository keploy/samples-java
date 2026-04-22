package com.tricentisdemo.sap.customer360.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of SAP's {@code A_BusinessPartnerRole}. A business partner can act
 * in several roles (customer {@code FLCU00}, supplier {@code FLVN00}, etc.)
 * simultaneously; this is the classic "one master record, many roles"
 * pattern at the heart of SAP master data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessPartnerRole {

    @JsonProperty("BusinessPartner")
    private String businessPartner;

    @JsonProperty("BusinessPartnerRole")
    private String roleCode;

    @JsonProperty("ValidFrom")
    private String validFrom;

    @JsonProperty("ValidTo")
    private String validTo;

    public String getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(String v) { this.businessPartner = v; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String v) { this.roleCode = v; }

    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String v) { this.validFrom = v; }

    public String getValidTo() { return validTo; }
    public void setValidTo(String v) { this.validTo = v; }
}
