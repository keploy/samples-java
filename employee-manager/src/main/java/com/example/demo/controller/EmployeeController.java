package com.example.demo.controller;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    // Get all employees
    @GetMapping(value = "/employees", produces = "application/json")
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    // Get employee by ID
    @GetMapping(value = "/employees/{id}", produces = "application/json")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable(value = "id") Long employeeId) throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
        return ResponseEntity.ok(employee);
    }

    // Create employee
    @PostMapping(value = "/employees", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody Employee employee) {
        employee.setTimestamp(Instant.now().getEpochSecond());
        Employee savedEmployee = employeeRepository.save(employee);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedEmployee.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedEmployee);
    }

    // Update employee
    @PutMapping(value = "/employees/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Employee> updateEmployee(@PathVariable(value = "id") Long employeeId,
                                                   @Valid @RequestBody Employee employeeDetails) throws ResourceNotFoundException {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));

        employee.setEmail(employeeDetails.getEmail());
        employee.setFirstName(employeeDetails.getFirstName());
        employee.setLastName(employeeDetails.getLastName());
        employee.setTimestamp(Instant.now().getEpochSecond());

        Employee updatedEmployee = employeeRepository.save(employee);
        return ResponseEntity.ok(updatedEmployee);
    }

    // Delete employee
    @DeleteMapping(value = "/employees/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable(value = "id") Long employeeId) throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));

        employeeRepository.delete(employee);
        return ResponseEntity.noContent().build();
    }
}
