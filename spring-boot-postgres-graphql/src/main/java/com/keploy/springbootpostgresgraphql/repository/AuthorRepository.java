package com.keploy.springbootpostgresgraphql.repository;

import com.keploy.springbootpostgresgraphql.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Integer> {

    Author findAuthorById(int id);

    List<Author> findAll();

}
