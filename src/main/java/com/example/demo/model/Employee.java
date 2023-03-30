package com.example.demo.model;

import com.example.demo.Validations.ValidUtf8;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
@Getter
@Setter
@Table(name = "employees")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @ValidUtf8
    @Column(name = "first_name")
    private String firstName;

    @NotBlank
    @ValidUtf8
    @Column(name = "last_name")
    private String lastName;

    @NotBlank
    @ValidUtf8
    @Column(name = "email")
    private String email;

    public Employee() {
        super();
    }
}
