/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.utils;

import java.util.UUID;

public class UniqueIdGenerator {
    private static final long twepoch = 1288834974657L;
    private static final long sequenceBits = 17L;
    private static final long sequenceMax = 65536L;
    private static volatile long lastTimestamp = -1L;
    private static volatile long sequence = 0L;

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private static synchronized Long generateLongId() {
        long timestamp = System.currentTimeMillis();
        if (lastTimestamp == timestamp) {
            if ((sequence = (sequence + 1L) % 65536L) == 0L) {
                timestamp = UniqueIdGenerator.tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        Long id = timestamp - 1288834974657L << 17 | sequence;
        return id;
    }

    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}

