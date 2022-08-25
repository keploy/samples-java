package com.example.demo.model;

import javax.persistence.*;

@Entity
@Table(name = "freshers")
public class Fresher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "full_name")
    private String fullName;
    @Column(name = "batch")
    private int batch;

    public Fresher() {
        super();
    }

    public Fresher(long id, String fullName, int batch) {
        this.id = id;
        this.fullName = fullName;
        this.batch = batch;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getBatch() {
        return batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }
}
