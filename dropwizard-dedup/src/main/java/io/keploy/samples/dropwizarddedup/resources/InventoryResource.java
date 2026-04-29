package io.keploy.samples.dropwizarddedup.resources;

import io.keploy.samples.dropwizarddedup.core.CatalogService;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private final CatalogService catalogService;

    public InventoryResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/healthz")
    public Map<String, Object> healthz() {
        return CatalogService.map("healthy", true);
    }

    @GET
    @Path("/catalog")
    public Map<String, Object> catalog(@QueryParam("category") String category,
                                       @QueryParam("limit") Integer limit) {
        return catalogService.catalog(category == null ? "books" : category, limit == null ? 2 : limit);
    }

    @GET
    @Path("/catalog/{sku}")
    public Map<String, Object> item(@PathParam("sku") String sku) {
        Map<String, Object> item = catalogService.item(sku);
        if (item == null) {
            throw new NotFoundException();
        }
        return item;
    }

    @GET
    @Path("/search")
    public Map<String, Object> search(@QueryParam("term") String term,
                                      @QueryParam("sort") String sort) {
        return catalogService.search(term == null ? "" : term, sort == null ? "relevance" : sort);
    }

    @GET
    @Path("/files/{path: .+}")
    public Map<String, Object> file(@PathParam("path") String path,
                                    @QueryParam("download") boolean download) {
        return CatalogService.map("requested_file", "/" + path, "download", download);
    }

    @GET
    @Path("/headers")
    public Map<String, Object> headers(@HeaderParam("X-Tenant") String tenant,
                                       @HeaderParam("X-Request-Id") String requestId) {
        return CatalogService.map(
                "tenant", tenant == null ? "default" : tenant,
                "requestId", requestId == null ? "missing" : requestId
        );
    }
}
