package com.keploy.sapdemo.customer360.model;

/**
 * Flat, list-friendly projection of a {@link BusinessPartner} for the
 * {@code GET /api/v1/customers} paged list endpoint. Keeps payloads small
 * for UI tables that only need an at-a-glance view.
 */
public class CustomerSummary {

    private String id;
    private String name;
    private String category;
    private boolean blocked;

    public CustomerSummary() {
    }

    public CustomerSummary(String id, String name, String category, boolean blocked) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.blocked = blocked;
    }

    public static CustomerSummary from(BusinessPartner bp) {
        String displayName = bp.getFullName();
        if (displayName == null || displayName.isBlank()) {
            displayName = bp.getOrganizationName();
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = (bp.getFirstName() + " " + bp.getLastName()).trim();
        }
        return new CustomerSummary(
            bp.getBusinessPartner(),
            displayName,
            bp.getCategory(),
            Boolean.TRUE.equals(bp.getBlocked())
        );
    }

    public String getId() { return id; }
    public void setId(String v) { this.id = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean v) { this.blocked = v; }
}
