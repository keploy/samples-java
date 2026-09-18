package io.keploy.productcatalog.web;

import io.keploy.productcatalog.model.Product;
import io.keploy.productcatalog.service.ProductService;
import io.keploy.productcatalog.web.dto.InventorySummaryResponse;
import io.keploy.productcatalog.web.dto.ProductRequest;
import io.keploy.productcatalog.web.dto.ProductResponse;
import io.keploy.productcatalog.web.dto.StockAdjustmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) String category) {
        return service.findAll(category).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/summary")
    public InventorySummaryResponse summary(
            @RequestParam(name = "lowStockThreshold", defaultValue = "5") int lowStockThreshold) {
        return service.getInventorySummary(lowStockThreshold);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return ProductResponse.from(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        Product created = service.create(request);
        URI location = uriBuilder.path("/api/products/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(created));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(service.update(id, request));
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse adjustStock(@PathVariable Long id,
                                       @Valid @RequestBody StockAdjustmentRequest request) {
        return ProductResponse.from(service.adjustStock(id, request.delta()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
