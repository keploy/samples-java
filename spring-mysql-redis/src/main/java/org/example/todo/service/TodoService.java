package org.example.todo.service;

import org.example.todo.model.Todo;
import org.example.todo.repository.TodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;
import java.util.Optional;

@Service
public class TodoService {

    @Autowired
    private TodoRepository todoRepository;

    @Cacheable(value = "todos", key = "#id")
    public Optional<Todo> findById(Long id) {
        return todoRepository.findById(id);
    }

    @Cacheable(value = "todos")
    public List<Todo> findAll() {
        return todoRepository.findAll();
    }

    @CachePut(value = "todos", key = "#todo.id")
    public Todo save(Todo todo) {
        return todoRepository.save(todo);
    }

    @CacheEvict(value = "todos", key = "#id")
    public void deleteById(Long id) {
        todoRepository.deleteById(id);
    }
}