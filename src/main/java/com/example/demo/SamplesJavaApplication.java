package com.example.demo;
//import io.keploy.regression.KeployInstance;
//import io.keploy.regression.keploy.AppConfig;
//import io.keploy.regression.keploy.Config;
//import io.keploy.regression.keploy.Keploy;
//import io.keploy.regression.keploy.ServerConfig;
//import io.keploy.servlet.middleware;
//import com.gkrosx.filter_dependency.middleware;

import io.keploy.regression.KeployInstance;
import io.keploy.regression.keploy.AppConfig;
import io.keploy.regression.keploy.Config;
import io.keploy.regression.keploy.Keploy;
import io.keploy.regression.keploy.ServerConfig;
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
        middleware md = new middleware();


        SpringApplication.run(SamplesJavaApplication.class, args);
    }

}

