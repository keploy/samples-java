package com.example.user.model;

import lombok.Data;
import lombok.Getter;
import lombok.*;
import jakarta.persistence.Entity;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;

@Entity
@Data
@Getter
@Setter
@ToString
@Table(name="todo")
@Accessors(chain = true)
public class User {

    @Id
    private long id;
    private String name;
    private Integer age;
    private String birthday;

}