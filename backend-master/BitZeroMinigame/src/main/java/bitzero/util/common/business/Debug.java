package bitzero.util.common.business;

import bitzero.server.config.ConfigHandle;
import org.apache.log4j.Logger;

public class Debug {
    public static boolean DEBUG = ConfigHandle.instance().getLong("debug_trace") == 1;
    public static Logger log = Logger.getLogger("debug");

    public static void trace(Object ... objs) {
        StringBuilder str = new StringBuilder();
        String separator = " ";
        for (Object o : objs) {
            str.append(separator).append(o);
        }
        log.debug(str.toString());
    }

    public static void info(Object ... objs) {
        StringBuilder str = new StringBuilder();
        String separator = " | ";
        for (Object o : objs) {
            str.append(separator).append(o);
        }
        log.info(str.toString());
    }

    public static  void warn(Object ... objs) {
        StringBuilder str = new StringBuilder();
        String separator = " | ";
        for (Object o : objs) {
            str.append(separator).append(o);
        }
        log.warn(str.toString());
    }

    public static void printStackTrace() {
        StackTraceElement[] objs = Thread.currentThread().getStackTrace();
        StringBuilder str = new StringBuilder();
        String separator = "\n";
        for (StackTraceElement o : objs) {
            str.append(separator).append(o);
        }
        Debug.trace(str.toString());
    }

    public static void system(Object ... objs) {
        StringBuilder str = new StringBuilder();
        String separator = " | ";
        for (Object o : objs) {
            str.append(separator).append(o);
        }
        log.fatal(str.toString());
    }

//    public static void trace(Object s) {
//        if (DEBUG) {
//            System.out.println(s);
//        }
//    }
}

