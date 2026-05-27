/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.epay.megacard;

public class Base64Utils {
    private static byte[] mBase64EncMap = new byte[]{65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47};
    private static byte[] mBase64DecMap;

    public static String base64Encode(byte[] aData) {
        int srcIndex;
        if (aData == null || aData.length == 0) {
            throw new IllegalArgumentException("Can not encode NULL or empty byte array.");
        }
        byte[] encodedBuf = new byte[(aData.length + 2) / 3 * 4];
        int destIndex = 0;
        for (srcIndex = 0; srcIndex < aData.length - 2; srcIndex += 3) {
            encodedBuf[destIndex++] = mBase64EncMap[aData[srcIndex] >>> 2 & 63];
            encodedBuf[destIndex++] = mBase64EncMap[aData[srcIndex + 1] >>> 4 & 15 | aData[srcIndex] << 4 & 63];
            encodedBuf[destIndex++] = mBase64EncMap[aData[srcIndex + 2] >>> 6 & 3 | aData[srcIndex + 1] << 2 & 63];
            encodedBuf[destIndex++] = mBase64EncMap[aData[srcIndex + 2] & 63];
        }
        if (srcIndex < aData.length) {
            encodedBuf[destIndex++] = mBase64EncMap[aData[srcIndex] >>> 2 & 63];
            if (srcIndex < aData.length - 1) {
                encodedBuf[destIndex++] = mBase64EncMap[aData[srcIndex + 1] >>> 4 & 15 | aData[srcIndex] << 4 & 63];
                encodedBuf[destIndex++] = mBase64EncMap[aData[srcIndex + 1] << 2 & 63];
            } else {
                encodedBuf[destIndex++] = mBase64EncMap[aData[srcIndex] << 4 & 63];
            }
        }
        while (destIndex < encodedBuf.length) {
            encodedBuf[destIndex] = 61;
            ++destIndex;
        }
        String result = new String(encodedBuf);
        return result;
    }

    public static byte[] base64Decode(String aData) {
        int destIndex;
        if (aData == null || aData.length() == 0) {
            throw new IllegalArgumentException("Can not decode NULL or empty string.");
        }
        byte[] data = aData.getBytes();
        int tail = data.length;
        while (data[tail - 1] == 61) {
            --tail;
        }
        byte[] decodedBuf = new byte[tail - data.length / 4];
        for (int i = 0; i < data.length; ++i) {
            data[i] = mBase64DecMap[data[i]];
        }
        int srcIndex = 0;
        for (destIndex = 0; destIndex < decodedBuf.length - 2; destIndex += 3) {
            decodedBuf[destIndex] = (byte)(data[srcIndex] << 2 & 255 | data[srcIndex + 1] >>> 4 & 3);
            decodedBuf[destIndex + 1] = (byte)(data[srcIndex + 1] << 4 & 255 | data[srcIndex + 2] >>> 2 & 15);
            decodedBuf[destIndex + 2] = (byte)(data[srcIndex + 2] << 6 & 255 | data[srcIndex + 3] & 63);
            srcIndex += 4;
        }
        if (destIndex < decodedBuf.length) {
            decodedBuf[destIndex] = (byte)(data[srcIndex] << 2 & 255 | data[srcIndex + 1] >>> 4 & 3);
        }
        if (++destIndex < decodedBuf.length) {
            decodedBuf[destIndex] = (byte)(data[srcIndex + 1] << 4 & 255 | data[srcIndex + 2] >>> 2 & 15);
        }
        return decodedBuf;
    }

    static {
        byte[] base64Map = mBase64EncMap;
        mBase64DecMap = new byte[128];
        for (int i = 0; i < mBase64EncMap.length; ++i) {
            Base64Utils.mBase64DecMap[Base64Utils.mBase64EncMap[i]] = (byte)i;
        }
    }
}

