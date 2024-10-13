package com.example.potionsapi.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;

@Configuration
public class MongoDbConfiguration {

    // private static final String CA_CERT_PATH = "/Users/gouravkumar/Desktop/Keploy/Lima-workspace/gk_workspace/tls-enabled/MongoDB/ca.crt";
    private static final String CA_CERT_PATH = "/Users/gouravkumar/Desktop/Keploy/Keploy-Server/current-keploy/keploy/pkg/core/proxy/asset/ca.crt";
    private final String host = "localhost";
    private final int port = 27017;
    private final String database = "admin";
    private final String username = "admin";
    private final String password = "password";

    @Bean
    @Primary
    @SneakyThrows
    @Profile("!local")
    public MongoClient mongoClient() {
        // Load the CA certificate and create the SSLContext
        SSLContext sslContext = createSSLContext(CA_CERT_PATH);

        // Get the SSL socket factory from the SSL context
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        // Configure MongoClientOptions with the SSL socket factory
        MongoClientOptions options = MongoClientOptions.builder()
                .sslEnabled(true)
                .sslInvalidHostNameAllowed(true) // Set to false if you have proper SAN entries
                .socketFactory(sslSocketFactory)
                .build();

        // Set up the MongoCredential
        MongoCredential credential = MongoCredential.createCredential(username, database, password.toCharArray());

        // Set up the MongoClient with the host, port, and options
         MongoClient mc = new MongoClient(new ServerAddress(host, port), Collections.singletonList(credential), options);
         return mc;
    }

    @SneakyThrows
    private SSLContext createSSLContext(String caCertPath) {
        // Load the CA certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (InputStream certInputStream = new FileInputStream(caCertPath)) {
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(certInputStream);

            // Create a KeyStore containing the CA certificate
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("caCert", caCert);

            // Create an SSL context that uses the KeyStore
            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
            return sslContext;
        }
    }
}