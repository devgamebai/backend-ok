package bitzero.util.logcontroller.business;

import org.slf4j.LoggerFactory;

// Scribe logging disabled - using SLF4J fallback
public class LogController {
    static ILogController _instance;
    static final Object lock = new Object();

    public static ILogController GetController() {
        if (_instance == null) {
            synchronized(lock) {
                if (_instance == null) {
                    _instance = new ILogController() {
                        private final org.slf4j.Logger logger = LoggerFactory.getLogger("LogController");
                        public void writeLog(ILogController.LogMode mode, String data) {
                            logger.info("[{}] {}", mode.value(), data);
                        }
                        public void writeLog(String category, String data) {
                            logger.info("[{}] {}", category, data);
                        }
                        public void writeLog(ILogController.LogMode mode, String data, String extra) {
                            logger.info("[{}] {} {}", mode.value(), data, extra);
                        }
                    };
                }
            }
        }
        return _instance;
    }
}
