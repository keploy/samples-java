package com.example.demo.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@SuppressWarnings("deprecation")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee testEmployee;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        // Clean up repository to ensure fresh data for each test
        employeeRepository.deleteAll();

        // Create and save test employee
        testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setEmail("john.doe@example.com");
        employeeRepository.save(testEmployee);  // save to generate ID
    }

    @Test
    public void testGetEmployeeById() {
        get("/api/employees/" + testEmployee.getId())
            .then()
            .statusCode(200)
            .body("id", equalTo((int) testEmployee.getId()))  // Ensure correct ID
            .body("firstName", equalTo("John"))
            .body("lastName", equalTo("Doe"))
            .body("email", equalTo("john.doe@example.com"));
    }

    @Test
    public void testUpdateEmployee() {
        String updatedEmployeeJson = "{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane.doe@example.com\"}";

        given()
            .contentType(ContentType.JSON)
            .body(updatedEmployeeJson)
        .when()
            .put("/api/employees/" + testEmployee.getId())
        .then()
            .statusCode(200)
            .body("firstName", equalTo("Jane"))
            .body("lastName", equalTo("Doe"))
            .body("email", equalTo("jane.doe@example.com"));
    }

    @Test
    public void testDeleteEmployee() {
        delete("/api/employees/" + testEmployee.getId())
            .then()
            .statusCode(200);
        
        // Verify that employee no longer exists
        get("/api/employees/" + testEmployee.getId())
            .then()
            .statusCode(404);
    }
}
