package com.example.demo.dto;

import com.example.demo.model.Employee;

/**
 * DTO for employee response data
 * Provides standardized API response format
 */
public class EmployeeResponseDTO {
    private long id;
    private String firstName;
    private String lastName;
    private String email;
    private long timestamp;

    //default constructor
    public EmployeeResponseDTO() {
    }

    //constructor with parameters
    public EmployeeResponseDTO(long id, String firstName, String lastName, String email, long timestamp) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.timestamp = timestamp;
    }

    //convert entity to DTO
    public static EmployeeResponseDTO fromEntity(Employee emp) {
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setId(emp.getId());
        dto.setFirstName(emp.getFirstName());
        dto.setLastName(emp.getLastName());
        dto.setEmail(emp.getEmail());
        dto.setTimestamp(emp.getTimestamp());
        return dto;
    }

    //getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 