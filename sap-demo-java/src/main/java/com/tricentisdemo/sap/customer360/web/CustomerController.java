package com.tricentisdemo.sap.customer360.web;

import com.tricentisdemo.sap.customer360.model.BusinessPartner;
import com.tricentisdemo.sap.customer360.model.Customer360View;
import com.tricentisdemo.sap.customer360.model.CustomerSummary;
import com.tricentisdemo.sap.customer360.service.Customer360AggregatorService;
import com.tricentisdemo.sap.customer360.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Inbound REST surface.
 *
 * <p>Four endpoints mirror the typical consumption pattern of a Customer 360
 * service from downstream CRM / portal / analytics:
 * <table>
 *   <tr><th>Endpoint</th><th>Use case</th><th>SAP calls</th></tr>
 *   <tr><td>GET /api/v1/customers</td><td>Paged list for UI grids</td><td>1</td></tr>
 *   <tr><td>GET /api/v1/customers/{id}</td><td>Single-entity lookup</td><td>1</td></tr>
 *   <tr><td>GET /api/v1/customers/{id}/360</td><td>Aggregated 360 view</td><td><b>3 parallel</b></td></tr>
 *   <tr><td>GET /api/v1/customers/count</td><td>KPI tiles / metrics</td><td>1</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/customers")
@Validated
@Tag(name = "Customers", description = "SAP Business Partner aggregation endpoints")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    // SAP Business Partner id is alphanumeric, up to 10 chars in the sandbox.
    private static final String ID_REGEX = "^[0-9A-Za-z]{1,10}$";

    private final CustomerService customerService;
    private final Customer360AggregatorService aggregatorService;

    public CustomerController(CustomerService customerService,
                              Customer360AggregatorService aggregatorService) {
        this.customerService = customerService;
        this.aggregatorService = aggregatorService;
    }

    @Operation(summary = "List customers (paged)")
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int top,
        @RequestParam(defaultValue = "0") @Min(0) int skip
    ) {
        log.info("GET /customers top={} skip={}", top, skip);
        List<CustomerSummary> items = customerService.listCustomers(top, skip);
        return ResponseEntity.ok(Map.of(
            "items", items,
            "page", Map.of("top", top, "skip", skip, "size", items.size())
        ));
    }

    @Operation(summary = "Get total customer count (KPI tile)")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> count() {
        log.info("GET /customers/count");
        long total = customerService.totalCount();
        return ResponseEntity.ok(Map.of("total", total));
    }

    @Operation(summary = "Get single customer master data")
    @GetMapping("/{id}")
    public ResponseEntity<BusinessPartner> getById(
        @PathVariable @NotBlank @Pattern(regexp = ID_REGEX) String id
    ) {
        log.info("GET /customers/{}", id);
        return ResponseEntity.ok(customerService.getById(id));
    }

    @Operation(summary = "Get aggregated Customer 360 view",
               description = "Fan-out aggregator — triggers 3 parallel SAP OData calls per request.")
    @GetMapping("/{id}/360")
    public ResponseEntity<Customer360View> get360(
        @PathVariable @NotBlank @Pattern(regexp = ID_REGEX) String id
    ) {
        log.info("GET /customers/{}/360", id);
        return ResponseEntity.ok(aggregatorService.aggregate(id));
    }
}
