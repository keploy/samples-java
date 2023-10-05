package com.sample_crud.dynamo_db_app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import io.keploy.utils.AssertKTests;
import io.keploy.utils.HaltThread;

@SpringBootTest
class DynamoDbAppApplicationTests {

	// @Test
    // public void TestKeploy() throws InterruptedException {

    //     CountDownLatch countDownLatch = HaltThread.getInstance().getCountDownLatch();
       

    //    new Thread(() -> {
    //        DynamoDbAppApplication.main(new String[]{""});
    //        countDownLatch.countDown();
    //    }).start();

    //    countDownLatch.await();
    //    assertTrue(AssertKTests.result(), "Keploy Test Result");
    // }

}
