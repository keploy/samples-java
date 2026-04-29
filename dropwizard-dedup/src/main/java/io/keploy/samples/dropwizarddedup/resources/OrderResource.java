package io.keploy.samples.dropwizarddedup.resources;

import io.keploy.samples.dropwizarddedup.core.CatalogService;
import io.keploy.samples.dropwizarddedup.core.OrderRequest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    private final CatalogService catalogService;

    public OrderResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @POST
    public Response create(OrderRequest request) {
        Map<String, Object> response = catalogService.order(
                request.getCustomer(),
                request.getSku(),
                request.getQuantity(),
                request.isPriority()
        );
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{orderId}")
    public Map<String, Object> status(@PathParam("orderId") String orderId,
                                      @QueryParam("expand") boolean expand) {
        return catalogService.orderStatus(orderId, expand);
    }

    @PUT
    @Path("/{orderId}")
    public Map<String, Object> update(@PathParam("orderId") String orderId,
                                      OrderRequest request) {
        return catalogService.updateOrder(orderId, request.getStatus());
    }

    @DELETE
    @Path("/{orderId}")
    public Map<String, Object> delete(@PathParam("orderId") String orderId) {
        return catalogService.deleteOrder(orderId);
    }
}
