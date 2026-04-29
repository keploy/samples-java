package io.keploy.samples.dropwizarddedup.resources;

import io.keploy.samples.dropwizarddedup.core.CatalogService;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/platform")
public class PlatformResource {

    @GET
    @Path("/routes/{region}/{zone}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> route(@PathParam("region") String region,
                                     @PathParam("zone") String zone) {
        return CatalogService.map("region", region, "zone", zone, "target", region + "-" + zone + "-api");
    }

    @POST
    @Path("/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> event(Map<String, Object> event) {
        Object type = event.get("type");
        return CatalogService.map("accepted", true, "type", type == null ? "unknown" : type, "normalized", true);
    }

    @GET
    @Path("/content/html")
    @Produces(MediaType.TEXT_HTML)
    public Response html() {
        return Response.ok("<h1>dropwizard</h1>", MediaType.TEXT_HTML_TYPE).build();
    }
}
