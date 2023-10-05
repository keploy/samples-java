package com.sample_crud.dynamo_db_app.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import com.sample_crud.dynamo_db_app.entity.Product;

@Repository
@EnableScan
public interface ProductRepository extends CrudRepository<Product,String> {
}
