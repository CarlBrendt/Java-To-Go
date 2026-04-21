package ru.mts.ip.workflow.engine.service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SimpleCrypt {

  private static final byte[] RAW = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
  private static final byte[] SALT = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
  
  private final Cipher enCrypter;
  private final Cipher deCrypter;
  
  public SimpleCrypt() {
    enCrypter = createEncrypter();
    deCrypter = createDecrypter();
  }
  
  public String decryp(String value) {
    try {
      byte[] encryptedBytes = Base64.getDecoder().decode(value);
      byte[] decryptedBytes = deCrypter.doFinal(encryptedBytes);
      return new String(decryptedBytes, StandardCharsets.UTF_8).trim();
    } catch (Exception ex) {
      return value;
    }
  }
  
  private Cipher createEncrypter() {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(RAW, "AES"), new IvParameterSpec(SALT));
      return cipher;
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private Cipher createDecrypter() {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(RAW, "AES"), new IvParameterSpec(SALT));
      return cipher;
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
  
  public Map<String, String> encryptValuesMap(Map<String, String> toEncrypt) {
    try {
      Map<String, String> encryptedValuesMap = new HashMap<>();
      for(Map.Entry<String, String> entry : toEncrypt.entrySet()) {
        String k = entry.getKey();
        String v = entry.getValue();
        encryptedValuesMap.put(k, Base64.getEncoder().encodeToString(enCrypter.doFinal(v.getBytes(StandardCharsets.UTF_8))));
      }
      return encryptedValuesMap;
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public Map<String, String> decryptValuesMap(Map<String, String> encrypted) {
    try {
      Map<String, String> encryptedValuesMap = new HashMap<>();
      for(Map.Entry<String, String> entry : encrypted.entrySet()) {
        String k = entry.getKey();
        String v = entry.getValue();
        encryptedValuesMap.put(k, new String(deCrypter.doFinal(Base64.getDecoder().decode(v)), StandardCharsets.UTF_8));
      }
      return encryptedValuesMap;
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public String encryp(String value) {
    try {
      byte[] encryptedBytes = value.getBytes(StandardCharsets.UTF_8);
      byte[] decryptedBytes = enCrypter.doFinal(encryptedBytes);
      return Base64.getEncoder().encodeToString(decryptedBytes);
    } catch (Exception ex) {
      return value;
    }
  }
  
}
