/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.api.entities;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class CSVUtils {
    private static final char DEFAULT_SEPARATOR = ',';

    public static void writeLine(Writer w, List<String> values) throws IOException {
        CSVUtils.writeLine(w, values, ',', ' ');
    }

    public static void writeLine(Writer w, List<String> values, char separators) throws IOException {
        CSVUtils.writeLine(w, values, separators, ' ');
    }

    private static String followCVSformat(String value) {
        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;
    }

    public static void writeLine(Writer w, List<String> values, char separators, char customQuote) throws IOException {
        boolean first = true;
        if (separators == ' ') {
            separators = (char)44;
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(separators);
            }
            if (customQuote == ' ') {
                sb.append(CSVUtils.followCVSformat(value));
            } else {
                sb.append(customQuote).append(CSVUtils.followCVSformat(value)).append(customQuote);
            }
            first = false;
        }
        sb.append("\n");
        w.append(sb.toString());
    }
}

