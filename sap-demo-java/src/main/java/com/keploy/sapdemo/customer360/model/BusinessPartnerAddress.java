package com.keploy.sapdemo.customer360.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of SAP's {@code A_BusinessPartnerAddress} entity used by the 360 view.
 * One business partner typically has 1..N addresses (billing, shipping,
 * registered office, etc.) keyed by {@code AddressID}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessPartnerAddress {

    @JsonProperty("BusinessPartner")
    private String businessPartner;

    @JsonProperty("AddressID")
    private String addressId;

    @JsonProperty("ValidityStartDate")
    private String validFrom;

    @JsonProperty("ValidityEndDate")
    private String validTo;

    @JsonProperty("StreetName")
    private String street;

    @JsonProperty("HouseNumber")
    private String houseNumber;

    @JsonProperty("CityName")
    private String city;

    @JsonProperty("PostalCode")
    private String postalCode;

    @JsonProperty("Country")
    private String country;

    @JsonProperty("Region")
    private String region;

    // ---- accessors ---------------------------------------------------------

    public String getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(String v) { this.businessPartner = v; }

    public String getAddressId() { return addressId; }
    public void setAddressId(String v) { this.addressId = v; }

    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String v) { this.validFrom = v; }

    public String getValidTo() { return validTo; }
    public void setValidTo(String v) { this.validTo = v; }

    public String getStreet() { return street; }
    public void setStreet(String v) { this.street = v; }

    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String v) { this.houseNumber = v; }

    public String getCity() { return city; }
    public void setCity(String v) { this.city = v; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String v) { this.postalCode = v; }

    public String getCountry() { return country; }
    public void setCountry(String v) { this.country = v; }

    public String getRegion() { return region; }
    public void setRegion(String v) { this.region = v; }
}
