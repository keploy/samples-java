package com.example.demo;

//import com.gkrosx.filter_dependency.middleware;

import io.github.cdimascio.dotenv.Dotenv;
import io.keploy.grpc.GrpcClient;
import io.keploy.regression.KeployInstance;
import io.keploy.regression.keploy.AppConfig;
import io.keploy.regression.keploy.Config;
import io.keploy.regression.keploy.Keploy;
import io.keploy.regression.keploy.ServerConfig;
import io.keploy.regression.mode;
import io.keploy.servlet.middleware;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.demo", "io.keploy.servlet"})
//@SpringBootApplication
//@SpringBootApplication(scanBasePackages = {"com.example.demo","com.gkrosx.filter_dependency"})
public class SamplesJavaApplication {
    public static void main(String[] args) {
        KeployInstance ki = KeployInstance.getInstance();
        Keploy kp = new Keploy();
        Config cfg = new Config();
        AppConfig appConfig = new AppConfig();
        appConfig.setName("samples_java");
        appConfig.setPort("8090");
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setURL("http://localhost:8081/api");
        cfg.setApp(appConfig);
        cfg.setServer(serverConfig);
        kp.setCfg(cfg);
        ki.setKeploy(kp);

        GrpcClient grpcClient = new GrpcClient();

        Dotenv dotenv = Dotenv.load();
        SpringApplication.run(SamplesJavaApplication.class, args);

        if (kp != null && dotenv.get("KEPLOY_MODE") != null && (dotenv.get("KEPLOY_MODE")).equals(new mode().getMode().MODE_TEST.getTypeName())) {
            try {
                grpcClient.Test();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


    }

}

