/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.gencode;

import java.util.Arrays;

public class CodeConfig {
    public static final char PATTERN_PLACEHOLDER = '#';
    private final int length;
    private final String charset;
    private final String prefix;
    private final String postfix;
    private final String pattern;

    public CodeConfig(Integer length, String charset, String prefix, String postfix, String pattern) {
        if (length == null) {
            length = 8;
        }
        if (charset == null) {
            charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        }
        if (pattern == null) {
            char[] chars = new char[length.intValue()];
            Arrays.fill(chars, '#');
            pattern = new String(chars);
        }
        this.length = length;
        this.charset = charset;
        this.prefix = prefix;
        this.postfix = postfix;
        this.pattern = pattern;
    }

    public static CodeConfig length(int length) {
        return new CodeConfig(length, null, null, null, null);
    }

    public static CodeConfig pattern(String pattern) {
        return new CodeConfig(null, null, null, null, pattern);
    }

    public int getLength() {
        return this.length;
    }

    public String getCharset() {
        return this.charset;
    }

    public CodeConfig withCharset(String charset) {
        return new CodeConfig(this.length, charset, this.prefix, this.postfix, this.pattern);
    }

    public String getPrefix() {
        return this.prefix;
    }

    public CodeConfig withPrefix(String prefix) {
        return new CodeConfig(this.length, this.charset, prefix, this.postfix, this.pattern);
    }

    public String getPostfix() {
        return this.postfix;
    }

    public CodeConfig withPostfix(String postfix) {
        return new CodeConfig(this.length, this.charset, this.prefix, postfix, this.pattern);
    }

    public String getPattern() {
        return this.pattern;
    }

    public String toString() {
        return "CodeConfig [length=" + this.length + ", charset=" + this.charset + ", prefix=" + this.prefix + ", postfix=" + this.postfix + ", pattern=" + this.pattern + "]";
    }

    public static class Charset {
        public static final String ALPHABETIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        public static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        public static final String NUMBERS = "0123456789";
    }

}

