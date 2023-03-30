import com.example.demo.SamplesJavaApplication;
import io.keploy.regression.Mode;
import io.keploy.utils.HaltThread;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SamplesJavaApplication_Test {

    @Test
    public void Test() {
        assertEquals(1,1);
    }


    @Test
    public void TestKeploy() throws InterruptedException {

        CountDownLatch countDownLatch = HaltThread.getInstance().getCountDownLatch();
        Mode.setTestMode();

        new Thread(() -> {
            SamplesJavaApplication.main(new String[]{""});
            countDownLatch.countDown();
        }).start();

        countDownLatch.await();
    }
}
