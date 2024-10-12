package com.example.spring_elastic.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.spring_elastic.client.BookElasticsearchClient;
import com.example.spring_elastic.models.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;


import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

@Configuration
public class BookElasticsearchService {
    @Autowired
    private BookElasticsearchClient client;
    private static final String BOOK_INDEX = "books";

    public Book createBook(Book book) {
        try {
            client.getClient().index(i -> i
                    .index(BOOK_INDEX)
                    .id(book.getId())
                    .document(book)
            );
            return book;
        } catch (ElasticsearchException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Book> findBookByName(String query)
    {
        List<Book> books = new ArrayList<>();

        try {
            SearchResponse<Book> search = client.getClient().search(s -> s
                            .index(BOOK_INDEX)
                            .query(q -> q
                                    .match(t -> t
                                            .field("name")
                                            .query(query))),
                    Book.class);

            List<Hit<Book>> hits = search.hits().hits();
            for (Hit<Book> hit: hits) {
                books.add(hit.source());
            }

            return books;
        } catch (ElasticsearchException exception) {
            exception.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}