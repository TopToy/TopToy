package config;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static config.stdErr.createLoggingProxy;

public class Config {
    static{
//        System.setErr(IoBuilder.forLogger(LogManager.getRootLogger()).buildPrintStream());
//        System.setOut(IoBuilder.forLogger(LogManager.getRootLogger()).buildPrintStream());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy:hh:m");
        System.setProperty("current.date.time", dateFormat.format(new Date()));
//        System.setErr(createLoggingProxy(System.err));

    }

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Config.class);
    public Config() {
        logger.debug("logger is configured");
    }


}
