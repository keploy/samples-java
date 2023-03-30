package com.example.demo.controller;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/")
public class EmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    //get employees
    @GetMapping("employees")
    public List<Employee> getAllEmployee() {
        logger.info("Received request to get all employee");
        return this.employeeRepository.findAll();
    }

    //get employee by id
    @GetMapping("employees/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable(value = "id") Long employeeId) throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
        return ResponseEntity.ok().body(employee);
    }

    //save employee
    @PostMapping("employees")
    public ResponseEntity<?> createEmployee(@Valid @RequestBody Employee employee) throws Exception {
        logger.info("Received request to create employee: {}", employee);
        return ResponseEntity.ok(this.employeeRepository.save(employee));
    }

    //update employee
    @PutMapping("employees/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable(value = "id") Long employeeId, @Valid @RequestBody Employee employeeDetails) throws ResourceNotFoundException {

        logger.info("Received request to update employee: {}", employeeDetails);


        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
        employee.setEmail(employeeDetails.getEmail());
        employee.setFirstName(employeeDetails.getFirstName());
        employee.setLastName(employeeDetails.getLastName());
        return ResponseEntity.ok(this.employeeRepository.save(employee));
    }

    //delete employee
    @DeleteMapping("employees/{id}")
    public Map<String, Boolean> deleteEmployee(@PathVariable(value = "id") Long employeeId) throws ResourceNotFoundException {
        logger.info("Received request to delete employee: {}", employeeId);
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
        this.employeeRepository.delete(employee);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);

        return response;
    }
}