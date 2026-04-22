package com.tricentisdemo.sap.customer360.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic envelope for an SAP OData v2 single-entity response:
 *
 * <pre>
 * { "d": { ...entity fields... } }
 * </pre>
 *
 * The {@code d} wrapper is SAP's OData v2 convention. OData v4 drops it and
 * returns the entity at the root — when this service is migrated to v4 APIs
 * (e.g., {@code /API_BUSINESS_PARTNER_SRV/A_BusinessPartner('11')} in
 * S/4HANA Cloud), this envelope goes away. That shift is the exact kind of
 * change a Tricentis migration customer has to regression-test.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ODataEntityResponse<T> {

    @JsonProperty("d")
    private T entity;

    public ODataEntityResponse() {
    }

    public T getEntity() {
        return entity;
    }

    public void setEntity(T entity) {
        this.entity = entity;
    }
}
