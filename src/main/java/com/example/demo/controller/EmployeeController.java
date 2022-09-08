package com.example.demo.controller;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;


@RestController
@RequestMapping("/api/")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    //get employee by id
    @GetMapping("employees/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable(value = "id") Long employeeId) throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
        return ResponseEntity.ok().body(employee);
    }

    //save employee
    @PostMapping("employees")
    public Employee createEmployee(@RequestBody Employee employee) {
        employee.setTimestamp(Instant.now().getEpochSecond());
        return this.employeeRepository.save(employee);
    }
}
