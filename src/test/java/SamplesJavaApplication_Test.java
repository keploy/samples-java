import com.example.demo.SamplesJavaApplication;
import io.keploy.regression.mode;
import io.keploy.utils.HaltThread;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

public class SamplesJavaApplication_Test {

    @Test
    public void TestKeploy() throws InterruptedException {

        CountDownLatch countDownLatch = HaltThread.getInstance().getCountDownLatch();
        mode.setTestMode();

        new Thread(() -> {
            SamplesJavaApplication.main(new String[]{""});
            countDownLatch.countDown();
        }).start();

        countDownLatch.await();
    }
}
