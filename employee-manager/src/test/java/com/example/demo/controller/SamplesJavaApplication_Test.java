import com.example.demo.SamplesJavaApplication;
import com.example.demo.controller.EmployeeController;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;

import io.keploy.cli.KeployCLI;
import io.keploy.cli.KeployCLI.TestRunStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.Order;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.http.HttpClient;
import java.util.Arrays;

import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SamplesJavaApplication_Test {

    @Test
    @Order(Integer.MAX_VALUE)
    public void testKeploy() throws IOException, InterruptedException {
        String jarPath = "target/springbootapp-0.0.1-SNAPSHOT.jar";
        String[] testSets = KeployCLI.FetchTestSets();

        if (testSets == null) {
            System.err.println("Test sets are null ");
            return;
        }

        System.out.println("TestSets: " + Arrays.asList(testSets));

        KeployCLI.runTestsAndCoverage(jarPath, testSets);
    }

}