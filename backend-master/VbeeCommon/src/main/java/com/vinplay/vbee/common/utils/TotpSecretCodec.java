package com.vinplay.vbee.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import org.apache.log4j.Logger;

public class TotpSecretCodec {
    private static final Logger logger = Logger.getLogger(TotpSecretCodec.class);
    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // in bits
    private static final int GCM_IV_LENGTH = 12; // in bytes
    
    // Fallback key if TOTP_ENCRYPTION_KEY is not set (32 bytes hex = 16 bytes raw for AES-128, or 32 bytes for AES-256)
    // We expect a 32-byte hex string (which decodes to 16 bytes) or a 64-byte hex string (which decodes to 32 bytes).
    private static byte[] secretKeyBytes;

    static {
        String envKey = System.getenv("TOTP_ENCRYPTION_KEY");
        if (envKey == null || envKey.isEmpty()) {
            throw new IllegalStateException(
                "TOTP_ENCRYPTION_KEY env var is required. Generate with: openssl rand -hex 32");
        }
        try {
            secretKeyBytes = hexStringToByteArray(envKey);
        } catch (Exception e) {
            throw new IllegalStateException("TOTP_ENCRYPTION_KEY must be a hex string", e);
        }
        if (secretKeyBytes.length != 16 && secretKeyBytes.length != 32) {
            throw new IllegalStateException(
                "TOTP_ENCRYPTION_KEY must decode to 16 or 32 bytes (got " + secretKeyBytes.length + ")");
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return null;
        
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, "AES");
            javax.crypto.spec.GCMParameterSpec gcmParameterSpec = new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // Prepend IV to ciphertext
            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.error("Error encrypting TOTP secret", e);
            return null;
        }
    }

    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return null;

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            
            if (decoded.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted payload: too short");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

            int ciphertextLength = decoded.length - GCM_IV_LENGTH;
            byte[] ciphertext = new byte[ciphertextLength];
            System.arraycopy(decoded, GCM_IV_LENGTH, ciphertext, 0, ciphertextLength);

            Cipher cipher = Cipher.getInstance(ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, "AES");
            javax.crypto.spec.GCMParameterSpec gcmParameterSpec = new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, "UTF-8");
        } catch (Exception e) {
            logger.error("Error decrypting TOTP secret", e);
            return null;
        }
    }
}
