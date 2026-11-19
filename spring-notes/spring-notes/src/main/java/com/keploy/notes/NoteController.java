package com.keploy.notes;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/notes")
public class NoteController {
    private final Map<Integer, Note> notes = new HashMap<>();

    @PostMapping
    public Note create(@RequestBody Note note) {
        notes.put(note.id, note);
        return note;
    }

    @GetMapping
    public Collection<Note> getAll() {
        return notes.values();
    }

    @GetMapping("/{id}")
    public Note get(@PathVariable int id) {
        return notes.get(id);
    }
}
