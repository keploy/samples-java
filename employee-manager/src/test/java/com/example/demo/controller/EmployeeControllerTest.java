package com.example.demo.controller;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class EmployeeControllerTest {

    @InjectMocks
    private EmployeeController employeeController;

    @Mock
    private EmployeeRepository employeeRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllEmployee() {
        Employee emp1 = new Employee();
        emp1.setId(1);
        emp1.setFirstName("John");
        emp1.setLastName("Doe");
        emp1.setEmail("john.doe@example.com");

        Employee emp2 = new Employee();
        emp2.setId(2);
        emp2.setFirstName("Jane");
        emp2.setLastName("Doe");
        emp2.setEmail("jane.doe@example.com");

        List<Employee> employeeList = Arrays.asList(emp1, emp2);

        when(employeeRepository.findAll()).thenReturn(employeeList);

        List<Employee> result = employeeController.getAllEmployee();

        assertEquals(2, result.size());
        assertEquals(emp1.getFirstName(), result.get(0).getFirstName());
        assertEquals(emp2.getFirstName(), result.get(1).getFirstName());
    }

    @Test
    public void testGetEmployeeById() throws ResourceNotFoundException {
        Employee emp = new Employee();
        emp.setId(1);
        emp.setFirstName("John");
        emp.setLastName("Doe");
        emp.setEmail("john.doe@example.com");

        when(employeeRepository.findById(1L)).thenReturn(java.util.Optional.of(emp));

        ResponseEntity<Employee> response = employeeController.getEmployeeById(1L);
        Employee result = response.getBody();

        assertEquals(1, result.getId());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john.doe@example.com", result.getEmail());
    }

    @Test
    public void testGetEmployeeById_NotFound() {
        when(employeeRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            employeeController.getEmployeeById(1L);
        });
    }
}
