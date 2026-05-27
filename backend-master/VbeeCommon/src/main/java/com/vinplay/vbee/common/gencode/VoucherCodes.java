/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.gencode;

import com.vinplay.vbee.common.gencode.CodeConfig;
import java.util.Random;

public final class VoucherCodes {
    private static final Random RND = new Random(System.currentTimeMillis());

    public static String generate(CodeConfig config) {
        StringBuilder sb = new StringBuilder();
        char[] chars = config.getCharset().toCharArray();
        char[] pattern = config.getPattern().toCharArray();
        if (config.getPrefix() != null) {
            sb.append(config.getPrefix());
        }
        for (int i = 0; i < pattern.length; ++i) {
            if (pattern[i] == '#') {
                sb.append(chars[RND.nextInt(chars.length)]);
                continue;
            }
            sb.append(pattern[i]);
        }
        if (config.getPostfix() != null) {
            sb.append(config.getPostfix());
        }
        return sb.toString();
    }
}

