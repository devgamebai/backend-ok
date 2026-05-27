package bitzero.engine.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Logging {
     private static final String NEW_LINE = System.getProperty("line.separator");
     private static final String TAB = "\t";

     public static void changeConsoleHandlerLevel(Level newLevel) {
          Logger rootLogger = Logger.getLogger("");
          Handler[] handlers = rootLogger.getHandlers();
          if (handlers.length > 0) {
               Handler firstHandler = handlers[0];
               if (firstHandler != null && firstHandler instanceof ConsoleHandler) {
                    firstHandler.setLevel(newLevel);
               }
          }

     }

     public static Level getConsoleHandlerLevel() {
          Level level = null;
          Logger rootLogger = Logger.getLogger("");
          Handler[] handlers = rootLogger.getHandlers();
          if (handlers.length > 0) {
               Handler firstHandler = handlers[0];
               if (firstHandler != null && firstHandler instanceof ConsoleHandler) {
                    level = firstHandler.getLevel();
               }
          }

          return level;
     }

     public static void changeConsoleHandlerFormatter(Formatter formatter) {
          Logger rootLogger = Logger.getLogger("");
          Handler[] handlers = rootLogger.getHandlers();
          if (handlers.length > 0) {
               Handler firstHandler = handlers[0];
               if (firstHandler == null || !(firstHandler instanceof ConsoleHandler)) {
                    throw new RuntimeException("Could not change the ConsoleHandler's formatter!");
               }

               firstHandler.setFormatter(formatter);
          }

     }

     public static void logStackTrace(org.slf4j.Logger logger, Throwable throwable) {
          logStackTrace(logger, throwable.toString(), throwable.getStackTrace());
     }

     public static void logStackTrace(org.slf4j.Logger logger, StackTraceElement[] stackTrace) {
          logStackTrace(logger, (String)null, stackTrace);
     }

     public static void logStackTrace(org.slf4j.Logger logger, String cause, StackTraceElement[] stackTrace) {
          StringBuilder sb = new StringBuilder(NEW_LINE);
          if (cause != null) {
               sb.append(cause).append(NEW_LINE);
          }

          StackTraceElement[] astacktraceelement = stackTrace;
          int j = stackTrace.length;

          for(int i = 0; i < j; ++i) {
               StackTraceElement element = astacktraceelement[i];
               sb.append("\t").append(element).append(NEW_LINE);
          }

          logger.warn(sb.toString());
     }

     public static String formatStackTrace(StackTraceElement[] elements) {
          StringBuilder sb = new StringBuilder();
          int j = elements.length;

          for(int i = 0; i < j; ++i) {
               StackTraceElement element = elements[i];
               sb.append(element).append(NEW_LINE);
          }

          return sb.toString();
     }
}
