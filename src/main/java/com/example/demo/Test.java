package com.example.demo;

import io.keploy.grpc.GrpcClient;
import io.keploy.regression.KeployInstance;
import io.keploy.regression.keploy.Keploy;
import io.keploy.regression.mode;

public class Test {
    public static void main(String[] args) {
        mode m = new mode();
        m.setTestMode();
        SamplesJavaApplication.main(new String[]{"gourav"});
        System.out.println("hello");
        GrpcClient grpcClient = new GrpcClient();

        Keploy kp = KeployInstance.getInstance().getKeploy();
        if (kp != null && mode.getMode() != null && (mode.getMode())==(mode.ModeType.MODE_TEST)) {
            try {
                grpcClient.Test();
            } catch (Exception e) {
                System.out.println("can not run test mode");
                throw new RuntimeException(e);
            }
        }
    }
}
