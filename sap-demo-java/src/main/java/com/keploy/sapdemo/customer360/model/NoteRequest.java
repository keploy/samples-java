package com.keploy.sapdemo.customer360.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/customers/{id}/notes}.
 */
public class NoteRequest {

    @NotBlank
    @Size(min = 1, max = 2000)
    private String body;

    private String author;

    public NoteRequest() {}

    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }

    public String getAuthor() { return author; }
    public void setAuthor(String v) { this.author = v; }
}
