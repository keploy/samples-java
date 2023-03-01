import com.example.demo.SamplesJavaApplication;
import com.example.demo.controller.EmployeeController;
import com.example.demo.repository.EmployeeRepository;
import io.keploy.regression.Mode;
import io.keploy.utils.AssertKTests;
import io.keploy.utils.HaltThread;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamplesJavaApplication_Test {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeController employeeController;

    @Test()
    public void TestKeploy() throws InterruptedException {

        CountDownLatch countDownLatch = HaltThread.getInstance().getCountDownLatch();
        Mode.setTestMode();

        new Thread(() -> {
            SamplesJavaApplication.main(new String[]{""});
            countDownLatch.countDown();
        }).start();

        countDownLatch.await();
        assertTrue(AssertKTests.result());
    }

//    @Test
//    public void testCreateEmployee() {
//        System.out.println("testing createEmployee controller");
//        // Create a new Employee object for the request body
////        Employee requestEmployee = new Employee();
////        requestEmployee.setFirstName("John");
////        requestEmployee.setLastName("Doe");
////        requestEmployee.setEmail("john.doe@example.com");
////
////        // Create a mock Employee object to return from the repository
////        Employee savedEmployee = new Employee();
////        savedEmployee.setId(1L);
////        savedEmployee.setFirstName("John");
////        savedEmployee.setLastName("Doe");
////        savedEmployee.setEmail("john.doe@example.com");
////        savedEmployee.setTimestamp(Instant.now().getEpochSecond());
////        when(employeeRepository.save(requestEmployee)).thenReturn(savedEmployee);
////
////        // Call the createEmployee method with the request Employee object
////        Employee resultEmployee = employeeController.createEmployee(requestEmployee);
////
////        // Verify that the repository save method was called with the request Employee object
////        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
////        verify(employeeRepository).save(captor.capture());
////        assertEquals(requestEmployee, captor.getValue());
//
//        // Verify that the returned Employee object matches the mock object returned from the repository
//        assertTrue(true);
//        System.out.println("Done executing this unit test case (createEmployee)");
//    }

}
