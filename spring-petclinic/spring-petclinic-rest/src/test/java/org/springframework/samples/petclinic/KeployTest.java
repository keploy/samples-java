package org.springframework.samples.petclinic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.keploy.cli.KeployCLI;

public class KeployTest {
    @Test
    @Order(Integer.MAX_VALUE)
    public void TestKeploy() throws IOException, InterruptedException {

        System.out.println("Running Keploy Tests...");

        Boolean testResult = true;
        long MAX_TIMEOUT = 600000; // 1m
        try {

            String[] testSets = KeployCLI.FetchTestSets();
            if (testSets == null) {
                System.err.println("Test sets are null ");
                return;
            }

            System.out.println("TestSets: " + Arrays.asList(testSets));

            System.out.println("starting user application");

            boolean result = true;
            for (String testset : testSets) {

                // running the test set.
                String testRunId = KeployCLI.RunTestSet(testset);


                String jarPath = "target/spring-petclinic-rest-3.0.2.jar";
                String[] command = {
                        "java",
                        "-jar",
                        jarPath
                };
                String userCmd = String.join(" ", command);

                KeployCLI.StartUserApplication(userCmd);

                KeployCLI.TestRunStatus testRunStatus = KeployCLI.TestRunStatus.PASSED;

                long startTime = System.currentTimeMillis();

                // Check status in every 2 seconds
                while (true) {
                    // Sleep for 2 seconds
                    Thread.sleep(2000);

                    testRunStatus = KeployCLI.FetchTestSetStatus(testRunId);

                    if (testRunStatus == KeployCLI.TestRunStatus.RUNNING) {
                        System.out.println("testRun still in progress");

                        // Check if the current time exceeds the start time by MAX_TIMEOUT
                        if (System.currentTimeMillis() - startTime > MAX_TIMEOUT) {
                            System.out.println("Timeout reached, exiting loop");
                            break;
                        }

                        continue;
                    }

                    break;
                }

                if (testRunStatus == KeployCLI.TestRunStatus.FAILED || testRunStatus == KeployCLI.TestRunStatus.RUNNING) {
                    System.out.println("testrun failed");
                    result = false;
                } else if (testRunStatus == KeployCLI.TestRunStatus.PASSED) {
                    System.out.println("testrun passed");
                    result = true;
                }

                System.out.println("TestResult of [" + testset + "]:" + result);
                testResult = testResult && result;
                KeployCLI.FindCoverage(testset);
                //Change This time if you have bigger codebase. Because it will take more time to dump the coverage
                Thread.sleep(5000);
                KeployCLI.StopUserApplication();
            }
        } catch (Exception e) {
            System.err.println("failed to execute keploy tests:" + e);
        } finally {
            System.out.println("Testing done with status:" + testResult);
        }

        assertTrue(testResult, "Keploy Test Result");
    }
}
