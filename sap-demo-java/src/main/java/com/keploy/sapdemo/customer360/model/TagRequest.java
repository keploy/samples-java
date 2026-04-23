package com.keploy.sapdemo.customer360.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/customers/{id}/tags}.
 */
public class TagRequest {

    @NotBlank
    @Size(min = 1, max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9_.\\-]{1,64}$", message = "tag must match [a-zA-Z0-9_.-]{1,64}")
    private String tag;

    private String createdBy;

    public TagRequest() {}

    public String getTag() { return tag; }
    public void setTag(String v) { this.tag = v; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
}
