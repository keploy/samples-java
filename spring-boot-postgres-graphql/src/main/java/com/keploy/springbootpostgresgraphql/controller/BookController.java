package com.keploy.springbootpostgresgraphql.controller;

import com.keploy.springbootpostgresgraphql.dto.AuthorInput;
import com.keploy.springbootpostgresgraphql.dto.BookInput;
import com.keploy.springbootpostgresgraphql.entity.Author;
import com.keploy.springbootpostgresgraphql.entity.Book;
import com.keploy.springbootpostgresgraphql.repository.AuthorRepository;
import com.keploy.springbootpostgresgraphql.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
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
    public Book getBookById(@Argument int id) {
        return bookRepository.findBookById(id);
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

    @MutationMapping
    public Book addBook(@Argument BookInput book) {
        Author author = authorRepository.findAuthorById(book.getAuthorId());
        Book newBook = new Book();
        newBook.setName(book.getName());
        newBook.setPageCount(book.getPageCount());
        newBook.setAuthor(author);
        return bookRepository.save(newBook);
    }

    @MutationMapping
    public Book updateBook(@Argument int id, @Argument BookInput book) {
        Book existingBook = bookRepository.findBookById(id);
        if (existingBook != null) {
            Author author = authorRepository.findAuthorById(book.getAuthorId());
            existingBook.setName(book.getName());
            existingBook.setPageCount(book.getPageCount());
            existingBook.setAuthor(author);
            return bookRepository.save(existingBook);
        }
        return null;
    }

    @MutationMapping
    public Boolean deleteBook(@Argument int id) {
        bookRepository.deleteById(id);
        return true;
    }

    @MutationMapping
    public Author addAuthor(@Argument AuthorInput author) {
        Author newAuthor = new Author();
        newAuthor.setFirstName(author.getFirstName());
        newAuthor.setLastName(author.getLastName());
        return authorRepository.save(newAuthor);
    }

    @MutationMapping
    public Boolean deleteAuthor(@Argument int id) {
        authorRepository.deleteById(id);
        return true;
    }
}
