Feature: Employee API Tests

  Background:
    * url 'http://localhost:8080/api'
    * header Accept = 'application/json'
    
  Scenario: Get all employees
    Given path 'employees'
    When method GET
    Then status 200
    And match response == '#array'
    And match each response contains { id: '#number', firstName: '#string', lastName: '#string' }

  Scenario: Create a new employee
    Given path 'employees'
    And request { firstName: 'John', lastName: 'Doe', email: 'john.doe@example.com' }
    When method POST
    Then status 200
    And match response contains { id: '#number', firstName: 'John', lastName: 'Doe', email: 'john.doe@example.com', timestamp: '#number' }
    * def employeeId = response.id

  Scenario: Get employee by ID
    # First create an employee
    Given path 'employees'
    And request { firstName: 'Jane', lastName: 'Smith', email: 'jane.smith@example.com' }
    When method POST
    Then status 200
    * def employeeId = response.id

    # Then fetch the created employee
    Given path 'employees', employeeId
    When method GET
    Then status 200
    And match response contains { id: '#(employeeId)', firstName: 'Jane', lastName: 'Smith', email: 'jane.smith@example.com', timestamp: '#number' }

  Scenario: Update an employee
    # First create an employee
    Given path 'employees'
    And request { firstName: 'Bob', lastName: 'Wilson', email: 'bob.wilson@example.com' }
    When method POST
    Then status 200
    * def employeeId = response.id

    # Then update the employee
    Given path 'employees', employeeId
    And request { firstName: 'Bob', lastName: 'Wilson-Updated', email: 'bob.wilson.updated@example.com' }
    When method PUT
    Then status 200
    And match response contains { id: '#(employeeId)', firstName: 'Bob', lastName: 'Wilson-Updated', email: 'bob.wilson.updated@example.com', timestamp: '#number' }

  Scenario: Delete an employee
    # First create an employee
    Given path 'employees'
    And request { firstName: 'Tom', lastName: 'Brown', email: 'tom.brown@example.com' }
    When method POST
    Then status 200
    * def employeeId = response.id

    # Then delete the employee
    Given path 'employees', employeeId
    When method DELETE
    Then status 200
    And match response == { deleted: true }

    # Verify employee is deleted
    Given path 'employees', employeeId
    When method GET
    Then status 404

  Scenario: Get non-existent employee
    Given path 'employees', '99999'
    When method GET
    Then status 404
    And match response contains { message: '#string' }

  # Updated test case for missing fields since your API accepts null email
  Scenario: Create employee with null email
    Given path 'employees'
    And request { firstName: 'Missing', lastName: 'Fields' }
    When method POST
    Then status 200
    And match response contains { id: '#number', firstName: 'Missing', lastName: 'Fields', email: null, timestamp: '#number' }

  Scenario Outline: Create multiple employees
    Given path 'employees'
    And request { firstName: '<firstName>', lastName: '<lastName>', email: '<email>' }
    When method POST
    Then status 200
    And match response contains { firstName: '<firstName>', lastName: '<lastName>', email: '<email>' }

    Examples:
      | firstName | lastName | email                    |
      | Alice    | Johnson  | alice.j@example.com      |
      | Charlie  | Davis    | charlie.d@example.com    |
      | Eve      | Anderson | eve.anderson@example.com |