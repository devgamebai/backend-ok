package com.vinplay.api.backend.processors.rebate;

import javax.servlet.http.HttpServletRequest;

/**
 * Shared utility for rebate processors — avoids code duplication.
 */
final class RebateProcessorHelper {

    private RebateProcessorHelper() {}

    static int intParam(HttpServletRequest req, String name, int defaultValue) {
        String val = req.getParameter(name);
        if (val == null || val.isEmpty()) return defaultValue;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return defaultValue; }
    }

    static long longParam(HttpServletRequest req, String name, long defaultValue) {
        String val = req.getParameter(name);
        if (val == null || val.isEmpty()) return defaultValue;
        try { return Long.parseLong(val); } catch (NumberFormatException e) { return defaultValue; }
    }

    static double doubleParam(HttpServletRequest req, String name, double defaultValue) {
        String val = req.getParameter(name);
        if (val == null || val.isEmpty()) return defaultValue;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return defaultValue; }
    }

    static String strParam(HttpServletRequest req, String name) {
        String val = req.getParameter(name);
        return (val != null && !val.isEmpty()) ? val.trim() : null;
    }
}
