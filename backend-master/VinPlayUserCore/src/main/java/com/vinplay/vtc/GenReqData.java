/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.apache.commons.codec.binary.Base64
 */
package com.vinplay.vtc;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class GenReqData {
    public String encrypt(String key, String data) throws Exception {
        Cipher cipher = Cipher.getInstance("TripleDES");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(key.getBytes(), 0, key.length());
        String keymd5 = new BigInteger(1, md5.digest()).toString(16).substring(0, 24);
        SecretKeySpec keyspec = new SecretKeySpec(keymd5.getBytes(), "TripleDES");
        cipher.init(1, keyspec);
        byte[] stringBytes = data.getBytes();
        byte[] raw = cipher.doFinal(stringBytes);
        String base64 = Base64.getEncoder().encodeToString(raw);
        return base64;
    }

    public String decrypt(String key, String data) throws Exception {
        Cipher cipher = Cipher.getInstance("TripleDES");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(key.getBytes(), 0, key.length());
        String keymd5 = new BigInteger(1, md5.digest()).toString(16).substring(0, 24);
        SecretKeySpec keyspec = new SecretKeySpec(keymd5.getBytes(), "TripleDES");
        cipher.init(2, keyspec);
        byte[] raw = Base64.getDecoder().decode(data);
        byte[] stringBytes = cipher.doFinal(raw);
        String result = new String(stringBytes);
        return result;
    }

    public String topupTelco(String ServiceCode, String Account, int Amount, String TransDate, String OrgTransID, String DataSign) {
        try {
            String xmlre = "&lt;?xml version=\"1.0\" encoding=\"utf-16\"?&gt;\n&lt;RequestData xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"&gt;\n  &lt;ServiceCode&gt;" + ServiceCode + "&lt;/ServiceCode&gt;\n  &lt;Account&gt;" + Account + "&lt;/Account&gt;\n  &lt;Amount&gt;" + Amount + "&lt;/Amount&gt;\n  &lt;TransDate&gt;" + TransDate + "&lt;/TransDate&gt;\n  &lt;OrgTransID&gt;" + OrgTransID + "&lt;/OrgTransID&gt;\n  &lt;DataSign&gt;" + DataSign + "&lt;/DataSign&gt;\n&lt;/RequestData&gt;";
            return xmlre;
        }
        catch (Exception ex) {
            return ex.toString();
        }
    }

    public String topupPartner(String ServiceCode, String Account, String Amount, String Quantity, String TransDate, String OrgTransID, String DataSign) {
        try {
            String xmlre = "&lt;?xml version=\"1.0\" encoding=\"utf-16\"?&gt;\n&lt;RequestData xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"&gt;\n  &lt;ServiceCode&gt;" + ServiceCode + "&lt;/ServiceCode&gt;\n  &lt;Account&gt;" + Account + "&lt;/Account&gt;\n  &lt;Amount&gt;" + Amount + "&lt;/Amount&gt;\n  &lt;Quantity&gt;" + Quantity + "&lt;/Quantity&gt;\n  &lt;TransDate&gt;" + TransDate + "&lt;/TransDate&gt;\n  &lt;OrgTransID&gt;" + OrgTransID + "&lt;/OrgTransID&gt;\n  &lt;DataSign&gt;" + DataSign + "&lt;/DataSign&gt;\n  &lt;Description&gt;" + Account + "|" + TransDate + "|" + Quantity + "|" + Amount + "&lt;/Description&gt;\n&lt;/RequestData&gt;";
            return xmlre;
        }
        catch (Exception ex) {
            return ex.toString();
        }
    }

    public String checkAccount(String ServiceCode, String Account, String Quantity, String DataSign) {
        try {
            String xmlre = "&lt;?xml version=\"1.0\" encoding=\"utf-16\"?&gt;\n&lt;RequestData xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"&gt;\n  &lt;ServiceCode&gt;" + ServiceCode + "&lt;/ServiceCode&gt;\n  &lt;Account&gt;" + Account + "&lt;/Account&gt;\n  &lt;Quantity&gt;" + Quantity + "&lt;/Quantity&gt;\n  &lt;DataSign&gt;" + DataSign + "&lt;/DataSign&gt;\n&lt;/RequestData&gt;";
            return xmlre;
        }
        catch (Exception ex) {
            return ex.toString();
        }
    }

    public String buyCard(String ServiceCode, int Amount, int Quantity, String TransDate, String OrgTransID, String DataSign) {
        try {
            String xmlre = "&lt;?xml version=\"1.0\" encoding=\"utf-16\"?&gt;\n&lt;RequestData xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"&gt;\n  &lt;ServiceCode&gt;" + ServiceCode + "&lt;/ServiceCode&gt;\n  &lt;Amount&gt;" + Amount + "&lt;/Amount&gt;\n  &lt;Quantity&gt;" + Quantity + "&lt;/Quantity&gt;\n  &lt;TransDate&gt;" + TransDate + "&lt;/TransDate&gt;\n  &lt;OrgTransID&gt;" + OrgTransID + "&lt;/OrgTransID&gt;\n  &lt;DataSign&gt;" + DataSign + "&lt;/DataSign&gt;\n&lt;/RequestData&gt;";
            return xmlre;
        }
        catch (Exception ex) {
            return ex.toString();
        }
    }

    public String getCard(String ServiceCode, int Amount, int Quantity, String OrgTransID, String DataSign) {
        try {
            String xmlre = "&lt;?xml version=\"1.0\" encoding=\"utf-16\"?&gt;\n&lt;RequestData xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"&gt;\n  &lt;ServiceCode&gt;" + ServiceCode + "&lt;/ServiceCode&gt;\n  &lt;Amount&gt;" + Amount + "&lt;/Amount&gt;\n  &lt;Quantity&gt;" + Quantity + "&lt;/Quantity&gt;\n  &lt;OrgTransID&gt;" + OrgTransID + "&lt;/OrgTransID&gt;\n  &lt;DataSign&gt;" + DataSign + "&lt;/DataSign&gt;\n&lt;/RequestData&gt;";
            return xmlre;
        }
        catch (Exception ex) {
            return ex.toString();
        }
    }

    public String getBalance(String DataSign) {
        try {
            String xmlre = "&lt;?xml version=\"1.0\" encoding=\"utf-16\"?&gt;\n&lt;RequestData xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"&gt;\n  &lt;DataSign&gt;" + DataSign + "&lt;/DataSign&gt;\n&lt;/RequestData&gt;";
            return xmlre;
        }
        catch (Exception ex) {
            return ex.toString();
        }
    }

    public String getQuantiyCard(String ServiceCode, String Amount, String Quantity, String OrgTransID, String DataSign) {
        try {
            String xmlre = "&lt;?xml version=\"1.0\" encoding=\"utf-16\"?&gt;\n&lt;RequestData xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"&gt;\n  &lt;ServiceCode&gt;" + ServiceCode + "&lt;/ServiceCode&gt;\n  &lt;Amount&gt;" + Amount + "&lt;/Amount&gt;\n  &lt;Quantity&gt;" + Quantity + "&lt;/Quantity&gt;\n  &lt;OrgTransID&gt;" + OrgTransID + "&lt;/OrgTransID&gt;\n  &lt;DataSign&gt;" + DataSign + "&lt;/DataSign&gt;\n&lt;/RequestData&gt;";
            return xmlre;
        }
        catch (Exception ex) {
            return ex.toString();
        }
    }

    public String getPromotionDate(String Quantity, String DataSign) {
        try {
            String xmlre = "&lt;?xml version=\"1.0\" encoding=\"utf-16\"?&gt;\n&lt;RequestData xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"&gt;\n  &lt;Quantity&gt;" + Quantity + "&lt;/Quantity&gt;\n  &ltDataSign&gt;" + DataSign + "&lt;/DataSign&gt;\n&lt;/RequestData&gt;";
            return xmlre;
        }
        catch (Exception ex) {
            return ex.toString();
        }
    }

    public String createSign(String data, String filePath) {
        try {
            File privKeyFile = new File(filePath);
            byte[] privKeyBytes = this.readFile(privKeyFile);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
            PrivateKey pk = keyFactory.generatePrivate(privSpec);
            Signature sg = Signature.getInstance("SHA1withRSA");
            sg.initSign(pk);
            sg.update(data.getBytes());
            byte[] bDS = sg.sign();
            return new String(org.apache.commons.codec.binary.Base64.encodeBase64((byte[])bDS));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    public boolean checkSign(String sign, String data, String publicKeyFile) {
        try {
            File pubKeyFile = new File(publicKeyFile);
            byte[] pubKeyBytes = this.readFile(pubKeyFile);
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey k = keyFactory.generatePublic(pubSpec);
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(k);
            signature.update(data.getBytes());
            return signature.verify(org.apache.commons.codec.binary.Base64.decodeBase64((byte[])sign.getBytes()));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public byte[] readFile(File file) throws IOException {
        FilterInputStream dis = null;
        try {
            dis = new DataInputStream(new FileInputStream(file));
            byte[] data = new byte[(int)file.length()];
            ((DataInputStream)dis).readFully(data);
            byte[] arrby = data;
            return arrby;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
            return null;
        }
    }
}

