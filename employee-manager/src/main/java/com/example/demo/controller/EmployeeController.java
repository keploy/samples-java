package com.example.demo.controller;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.dto.EmployeeRequestDTO;
import com.example.demo.dto.EmployeeResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    //get all employees with filtering and sorting
    @GetMapping("employees")
    public List<EmployeeResponseDTO> getAllEmployee(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        //create sort object
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        //build specification for filtering
        Specification<Employee> spec = Specification.where(null);
        
        //filter by name (first name or last name)
        if (name != null) {
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), "%" + name.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("lastName")), "%" + name.toLowerCase() + "%")
            ));
        }
        
        //filter by email
        if (email != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }
        
        //get filtered and sorted employees
        List<Employee> employees = employeeRepository.findAll(spec, sort);
        
        //apply pagination
        employees = employees.stream().skip(page * size).limit(size).collect(Collectors.toList());
        
        //convert to DTOs and return
        return employees.stream().map(EmployeeResponseDTO::fromEntity).collect(Collectors.toList());
    }

    //get employee by id
    @GetMapping("employees/{id}")
    public ResponseEntity<EmployeeResponseDTO> getEmployeeById(@PathVariable(value = "id") Long employeeId) throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
        return ResponseEntity.ok(EmployeeResponseDTO.fromEntity(employee));
    }

    //create new employee
    @PostMapping("employees")
    public EmployeeResponseDTO createEmployee(@RequestBody EmployeeRequestDTO employeeDTO) {
        Employee employee = employeeDTO.toEntity();
        employee.setTimestamp(Instant.now().getEpochSecond());
        Employee saved = this.employeeRepository.save(employee);
        return EmployeeResponseDTO.fromEntity(saved);
    }

    //update existing employee
    @PutMapping("employees/{id}")
    public ResponseEntity<EmployeeResponseDTO> updateEmployee(@PathVariable(value = "id") Long employeeId, @Valid @RequestBody EmployeeRequestDTO employeeDetails) throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
        employee.setEmail(employeeDetails.getEmail());
        employee.setFirstName(employeeDetails.getFirstName());
        employee.setLastName(employeeDetails.getLastName());
        employee.setTimestamp(Instant.now().getEpochSecond());
        Employee updated = this.employeeRepository.save(employee);
        return ResponseEntity.ok(EmployeeResponseDTO.fromEntity(updated));
    }

    //delete employee
    @DeleteMapping("employees/{id}")
    public Map<String, Boolean> deleteEmployee(@PathVariable(value = "id") Long employeeId) throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
        this.employeeRepository.delete(employee);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return response;
    }
}