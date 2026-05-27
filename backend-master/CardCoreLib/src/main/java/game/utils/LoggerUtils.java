/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package game.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtils {
    public static /* varargs */ void info(String logger, Object ... objs) {
        Logger log = LoggerFactory.getLogger((String)logger);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < objs.length; ++i) {
            sb.append(objs[i]).append(" ");
        }
        log.info(sb.toString());
    }

    public static /* varargs */ void debug(String logger, Object ... objs) {
        Logger log = LoggerFactory.getLogger((String)logger);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < objs.length; ++i) {
            sb.append(objs[i]).append(" ");
        }
        log.debug(sb.toString());
    }

    public static /* varargs */ void error(String logger, Object ... objs) {
        Logger log = LoggerFactory.getLogger((String)logger);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < objs.length; ++i) {
            sb.append(objs[i]).append(" ");
        }
        log.error(sb.toString());
    }
}

