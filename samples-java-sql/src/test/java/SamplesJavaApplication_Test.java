import com.example.demo.SamplesJavaApplication;
import com.example.demo.controller.EmployeeController;
import com.example.demo.repository.EmployeeRepository;
import io.keploy.regression.Mode;
import io.keploy.utils.AssertKTests;
import io.keploy.utils.HaltThread;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;


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
}
