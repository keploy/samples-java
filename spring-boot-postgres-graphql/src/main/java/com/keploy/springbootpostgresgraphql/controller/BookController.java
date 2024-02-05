package com.keploy.springbootpostgresgraphql.controller;

import com.keploy.springbootpostgresgraphql.entity.Author;
import com.keploy.springbootpostgresgraphql.entity.Book;
import com.keploy.springbootpostgresgraphql.repository.AuthorRepository;
import com.keploy.springbootpostgresgraphql.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class BookController {

    @Autowired
    BookRepository bookRepository;
    @Autowired
    AuthorRepository authorRepository;

    @QueryMapping
    public Book getBookByName(@Argument String name) {
        return bookRepository.findBookByName(name);
    }

    @QueryMapping
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @QueryMapping
    public Author getAuthorById(@Argument int id) {
        return authorRepository.findAuthorById(id);
    }

    @QueryMapping
    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }


}
