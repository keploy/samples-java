package com.example.demo.dto;

import com.example.demo.model.Employee;

/**
 * DTO for handling employee creation and update requests
 * Provides clean separation between API layer and domain model
 */
public class EmployeeRequestDTO {
    private String firstName;
    private String lastName;
    private String email;
    // Optionally, add department if needed in future

    //default constructor
    public EmployeeRequestDTO() {
    }

    //constructor with parameters
    public EmployeeRequestDTO(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    //convert DTO to entity
    public Employee toEntity() {
        Employee emp = new Employee();
        emp.setFirstName(this.firstName);
        emp.setLastName(this.lastName);
        emp.setEmail(this.email);
        return emp;
    }

    //getters and setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
} 