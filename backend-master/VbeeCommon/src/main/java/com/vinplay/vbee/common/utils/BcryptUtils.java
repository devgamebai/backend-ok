package com.vinplay.vbee.common.utils;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Password hashing utilities. Supports bcrypt-style verification
 * with MD5 backward compatibility.
 *
 * TODO: Add jBCrypt dependency for proper bcrypt support.
 * Currently uses SHA-256 with salt as an improvement over MD5.
 */
public class BcryptUtils {

    public static String hashPassword(String plaintext) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(plaintext.getBytes("UTF-8"));
            // Format: $sha256$<salt-hex>$<hash-hex>
            return "$sha256$" + bytesToHex(salt) + "$" + bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public static boolean checkPassword(String plaintext, String stored) {
        if (stored == null) return false;
        // Legacy MD5 (32 hex chars, no $ prefix)
        if (!stored.startsWith("$")) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(plaintext.getBytes("UTF-8"));
                String md5 = bytesToHex(digest);
                return md5.equals(stored);
            } catch (Exception e) {
                return false;
            }
        }
        // SHA-256 with salt
        if (stored.startsWith("$sha256$")) {
            try {
                String[] parts = stored.split("\\$");
                // parts: ["", "sha256", salt, hash]
                byte[] salt = hexToBytes(parts[2]);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(salt);
                byte[] hash = md.digest(plaintext.getBytes("UTF-8"));
                return parts[3].equals(bytesToHex(hash));
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static boolean needsMigration(String stored) {
        return stored != null && !stored.startsWith("$");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
