package org.example.todo.controller;

import org.example.todo.model.Todo;
import org.example.todo.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @Autowired
    private DataSource dataSource;

    @GetMapping
    public List<Todo> getAllTodos() {
        return todoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> getTodoById(@PathVariable Long id) {
        Optional<Todo> todo = todoService.findById(id);
        return todo.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Todo createTodo(@RequestBody Todo todo) {
        return todoService.save(todo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Todo> updateTodo(@PathVariable Long id, @RequestBody Todo todoDetails) {
        Optional<Todo> todo = todoService.findById(id);
        if (todo.isPresent()) {
            Todo updatedTodo = todo.get();
            updatedTodo.setTitle(todoDetails.getTitle());
            updatedTodo.setDescription(todoDetails.getDescription());
            updatedTodo.setCompleted(todoDetails.isCompleted());
            todoService.save(updatedTodo);
            return ResponseEntity.ok(updatedTodo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        todoService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Re-executes a server-side prepared statement {@code n} times on the same
     * connection. With {@code useServerPrepStmts=true} and
     * {@code useCursorFetch=true} (set in application.properties) plus a
     * non-zero fetchSize, MySQL Connector/J 8.x sends a COM_STMT_RESET packet
     * between re-executions to clear the cursor state. This endpoint exists to
     * exercise that code path so Keploy record/replay can capture and match
     * COM_STMT_RESET frames.
     */
    @GetMapping("/stmt-reset/{id}/{n}")
    public ResponseEntity<Map<String, Object>> stmtReset(@PathVariable Long id,
                                                         @PathVariable int n) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT id, title, description, completed FROM todo WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Forces server-side cursor (with useCursorFetch=true), which is
            // what causes Connector/J to emit COM_STMT_RESET on re-execution.
            ps.setFetchSize(1);

            for (int i = 0; i < n; i++) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", rs.getLong("id"));
                        row.put("title", rs.getString("title"));
                        row.put("description", rs.getString("description"));
                        row.put("completed", rs.getBoolean("completed"));
                        rows.add(row);
                    }
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("iterations", n);
        response.put("rowsFetched", rows.size());
        response.put("results", rows);
        return ResponseEntity.ok(response);
    }
}
