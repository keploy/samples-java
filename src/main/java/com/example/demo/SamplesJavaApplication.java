package com.example.demo;

import io.github.cdimascio.dotenv.Dotenv;
import io.keploy.client.GrpcClient;
import io.keploy.regression.KeployInstance;
import io.keploy.regression.keploy.AppConfig;
import io.keploy.regression.keploy.Config;
import io.keploy.regression.keploy.Keploy;
import io.keploy.regression.keploy.ServerConfig;
import io.keploy.regression.mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.demo", "io.keploy.servlet"})
//@SpringBootApplication
public class SamplesJavaApplication {
    public static void main(String[] args) {
        KeployInstance ki = KeployInstance.getInstance();
        Keploy kp = new Keploy();
        Config cfg = new Config();
        AppConfig appConfig = new AppConfig();
        appConfig.setName("samples_java1");
        appConfig.setPort("8090");
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setURL("http://localhost:8081/api");
        cfg.setApp(appConfig);
        cfg.setServer(serverConfig);
        kp.setCfg(cfg);
        ki.setKeploy(kp);

        SpringApplication.run(SamplesJavaApplication.class, args);

        GrpcClient grpcClient = new GrpcClient();
        Dotenv dotenv = Dotenv.load();

        if (kp != null && dotenv.get("KEPLOY_MODE") != null && (dotenv.get("KEPLOY_MODE")).equals(mode.ModeType.MODE_TEST.getTypeName())) {
            try {
                grpcClient.Test();
            } catch (Exception e) {
                System.out.println("can not run test mode");
                throw new RuntimeException(e);
            }
        }
    }
}

