import com.example.demo.SamplesJavaApplication;
import io.keploy.regression.Mode;
import io.keploy.utils.AssertKTests;
import io.keploy.utils.HaltThread;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SamplesJavaApplication_Test {

    @Test
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
