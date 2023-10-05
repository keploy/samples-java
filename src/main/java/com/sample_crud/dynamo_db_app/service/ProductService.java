package com.sample_crud.dynamo_db_app.service;

import java.util.List;
import com.sample_crud.dynamo_db_app.entity.Product;

public interface ProductService {
    List<Product> getProductList();
    Product saveProduct(Product product);
    Product getProductById(String id);
    Product updateProduct(String id,Product product);
    void deleteProduct(String id);
}
