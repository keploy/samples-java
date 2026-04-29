package io.keploy.samples.dropwizarddedup.errors;

import io.keploy.samples.dropwizarddedup.core.CatalogService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ApiExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response source = exception.getResponse();
        int status = source == null ? 500 : source.getStatus();
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(CatalogService.map("error", status == 404 ? "not_found" : "request_failed", "status", status))
                .build();
    }
}
