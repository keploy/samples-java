package com.keploy.springbootpostgresgraphql.repository;

import com.keploy.springbootpostgresgraphql.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Category findCategoryById(int id);
    Category findCategoryByName(String name);
    List<Category> findAll();
}
