/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.gencode;

import com.vinplay.vbee.common.gencode.CodeConfig;
import com.vinplay.vbee.common.gencode.VoucherCodes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CodeGenerator {
    private static Set<String> generatedCode = new HashSet<String>();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static Set<String> genCode(int quantity, int length, String prefix, String postfix) {
        Set<String> set = generatedCode;
        synchronized (set) {
            HashSet<String> codes = new HashSet<String>();
            CodeConfig config = CodeConfig.length(length).withPrefix(prefix).withPostfix(postfix);
            for (int i = 0; i < quantity; ++i) {
                String code = null;
                while ((code = VoucherCodes.generate(config)) == null || generatedCode.contains(code) || codes.contains(code)) {
                }
                codes.add(code);
            }
            generatedCode.addAll(codes);
            return codes;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void loadGeneratedcode(Set<String> allCode) {
        Set<String> set = generatedCode;
        synchronized (set) {
            for (String code : allCode) {
                if (code == null || generatedCode.contains(code)) continue;
                generatedCode.add(code);
            }
        }
    }
}

