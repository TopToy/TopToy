package config;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Config {
    static{

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy:hh:m");
        System.setProperty("current.date.time", dateFormat.format(new Date()));
    }

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Config.class);
    public Config() {
        logger.debug("logger is configured");
    }
}
