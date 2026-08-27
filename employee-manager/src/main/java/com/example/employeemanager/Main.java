package com.example.employeemanager;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        EmployeeDAO employeeDAO = new EmployeeDAO();

        // Add a new employee
        Employee emp1 = new Employee(1, "John Doe", "Engineering", 75000);
        employeeDAO.addEmployee(emp1);

        // Retrieve all employees
        List<Employee> employees = employeeDAO.getAllEmployees();
        System.out.println("All Employees:");
        for (Employee emp : employees) {
            System.out.println(emp);
        }

        // Update an employee
        emp1.setSalary(80000);
        employeeDAO.updateEmployee(emp1);

        // Delete an employee
        employeeDAO.deleteEmployee(1);
    }
}