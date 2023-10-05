package com.sample_crud.dynamo_db_app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sample_crud.dynamo_db_app.entity.Product;
import com.sample_crud.dynamo_db_app.service.ProductService;
import java.util.List;

@RestController
// @RequestMapping("/api")

public class ProductController {

    // @GetMapping("/hello")
    // public String sayHello() {
    //     return "Hello, World!";
    // }


    @Autowired
    ProductService productService;

    @PostMapping("/product")
    public ResponseEntity<Product> saveProduct(@RequestBody Product product){
        return ResponseEntity.ok(productService.saveProduct(product));
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id){
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProductList(){
        return ResponseEntity.ok(productService.getProductList());
    }
    @PutMapping("/product/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id,@RequestBody Product product){
        return ResponseEntity.ok(productService.updateProduct(id,product));
    }
    @DeleteMapping("/product/{id}")
    public void deleteProductById(@PathVariable String id){
        productService.deleteProduct(id);
    }
}
