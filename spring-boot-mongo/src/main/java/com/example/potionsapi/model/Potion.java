package com.example.potionsapi.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.UUID;

@Document(collection = "PotionsDB")
public class Potion {

    @Id
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Indexed(unique = true)
    private String name;

    @Size(max = 200)
    private String description;

    @NotBlank
    private int bottle;
    @Max(5)         //quantity in ml
    private int quantity;


    //getters and setters
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public int getBottle() {
        return bottle;
    }
    public void setBottle(int bottle) {
        this.bottle = bottle;
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
