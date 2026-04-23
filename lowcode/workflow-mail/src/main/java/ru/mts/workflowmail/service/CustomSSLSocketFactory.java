package ru.mts.workflowmail.service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CustomSSLSocketFactory extends SSLSocketFactory {
  private final SSLSocketFactory factory;

  public CustomSSLSocketFactory(SSLContext sslContext) {
    this.factory = sslContext.getSocketFactory();
  }

  public static CustomSSLSocketFactory create(String pemCertificate) throws RuntimeException{
    SSLContext sslContext = null;
    try {
      sslContext = createSSLContext(pemCertificate);
    } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException |
        KeyManagementException e) {
      throw new RuntimeException(e);
    }
    return new CustomSSLSocketFactory(sslContext);
  }

  public static SSLContext createSSLContext(String pemCertificate)
      throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException,
      KeyManagementException {
    // Очищаем PEM от лишних символов
    String cleanCert = pemCertificate
        .replace("-----BEGIN CERTIFICATE-----", "")
        .replace("-----END CERTIFICATE-----", "")
        .replaceAll("\\s", "");

    // Декодируем Base64
    byte[] certBytes = java.util.Base64.getDecoder().decode(cleanCert);

    // Создаем сертификат
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    X509Certificate certificate = (X509Certificate) cf.generateCertificate(
        new ByteArrayInputStream(certBytes));

    // Создаем truststяore в памяти
    java.security.KeyStore trustStore = java.security.KeyStore.getInstance(KeyStore.getDefaultType());
    trustStore.load(null, null);
    trustStore.setCertificateEntry("custom-cert", certificate);

    // Создаем TrustManager
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);

    // Создаем SSLContext
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, tmf.getTrustManagers(), null);

    return sslContext;
  }

  @Override
  public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
    return factory.createSocket(s, host, port, autoClose);
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return factory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return factory.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    return factory.createSocket(host, port);
  }

  @Override
  public Socket createSocket(String host, int port, java.net.InetAddress localHost, int localPort) throws IOException {
    return factory.createSocket(host, port, localHost, localPort);
  }

  @Override
  public Socket createSocket(java.net.InetAddress host, int port) throws IOException {
    return factory.createSocket(host, port);
  }

  @Override
  public Socket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort) throws IOException {
    return factory.createSocket(address, port, localAddress, localPort);
  }
}
