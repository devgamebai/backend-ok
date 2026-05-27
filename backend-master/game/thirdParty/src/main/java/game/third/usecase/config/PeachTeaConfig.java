/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.config;

public class PeachTeaConfig {
    private String url;
    private String secretKey;
    private String encryptionKey;
    private String sha256Key;

    public PeachTeaConfig(String url, String secretKey, String encryptionKey, String sha256Key) {
        this.url = url;
        this.secretKey = secretKey;
        this.encryptionKey = encryptionKey;
        this.sha256Key = sha256Key;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getEncryptionKey() {
        return this.encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getSha256Key() {
        return this.sha256Key;
    }

    public void setSha256Key(String sha256Key) {
        this.sha256Key = sha256Key;
    }
}

