package com.sample_crud.dynamo_db_app.config;

// import java.net.URL;
// import java.security.SecureRandom;
// import java.security.cert.CertificateException;
// import java.security.cert.X509Certificate;
// import javax.net.ssl.HostnameVerifier;
// import javax.net.ssl.HttpsURLConnection;
// import javax.net.ssl.KeyManager;
// import javax.net.ssl.SSLContext;
// import javax.net.ssl.SSLSession;
// import javax.net.ssl.TrustManager;
// import javax.net.ssl.X509TrustManager;

// public class SSLTest {
    
//     public static void main(String [] args) throws Exception {
//         // configure the SSLContext with a TrustManager
//         SSLContext ctx = SSLContext.getInstance("TLS");
//         ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
//         SSLContext.setDefault(ctx);

//         URL url = new URL("https://mms.nw.ru");
//         HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
//         conn.setHostnameVerifier(new HostnameVerifier() {
//             @Override
//             public boolean verify(String arg0, SSLSession arg1) {
//                 return true;
//             }
//         });
//         System.out.println(conn.getResponseCode());
//         conn.disconnect();
//     }
    
//     private static class DefaultTrustManager implements X509TrustManager {

//         @Override
//         public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

//         @Override
//         public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

//         @Override
//         public X509Certificate[] getAcceptedIssuers() {
//             return null;
//         }
//     }
// }

