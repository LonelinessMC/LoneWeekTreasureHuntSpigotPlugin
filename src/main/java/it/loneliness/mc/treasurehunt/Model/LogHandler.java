package it.loneliness.mc.treasurehunt.Model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogHandler {
    private static LogHandler instance;
    private final Logger logger;
    private boolean debug = false;

    /**
     * Provides logging functionality
     * Debugging requires increasing the logging level, since the console never listens to logs below INFO
     * due to how parent loggers are hardcoded
     */
    private LogHandler(Logger logger) {
        this.logger = logger;
        log("Logging ready");
    }

    /**
     * Returns the single instance of LogHandler, creating it if necessary.
     * @param logger the Logger instance to use
     * @return the single instance of LogHandler
     */
    public static synchronized LogHandler getInstance(Logger logger) {
        if (instance == null) {
            instance = new LogHandler(logger);
        }
        return instance;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        if (debug) {
            log("Debugging is ON");
        }
    }

    public void log(String msg, Level loggingLevel) {
        logger.log(loggingLevel, msg);
    }

    public void log(String msg) {
        log(msg, debug ? Level.INFO : Level.CONFIG);
    }

    public void debug(String msg) {
        if(this.debug)
            log(msg, Level.INFO);
    }

    public void info(String msg) {
        log(msg, Level.INFO);
    }

    public void warning(String msg) {
        log(msg, Level.WARNING);
    }

    public void severe(String msg) {
        log(msg, Level.SEVERE);
    }

    public void severe(Exception e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        this.severe(stackTrace);
    }
}
