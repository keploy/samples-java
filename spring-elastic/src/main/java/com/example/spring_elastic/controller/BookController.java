package com.example.spring_elastic.controller;

import com.example.spring_elastic.models.Book;
import com.example.spring_elastic.service.BookElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "book")
public class BookController {

    @Autowired
    BookElasticsearchService bookElasticsearchService;

    @GetMapping("/find_by_name")
    ResponseEntity<Iterable<Book>> getBooksByName(@RequestParam("name") String query)
    {
        Iterable<Book> booksFromElasticSearch = bookElasticsearchService.findBookByName(query);

        return ResponseEntity.ok(booksFromElasticSearch);
    }

    @PostMapping("/create")
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book createdBook = bookElasticsearchService.createBook(book);
        return ResponseEntity.ok(createdBook);
    }
}