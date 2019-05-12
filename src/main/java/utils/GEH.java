package utils;

import org.apache.commons.lang.exception.ExceptionUtils;

public class GEH implements Thread.UncaughtExceptionHandler {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GEH.class);

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        logger.error(String.format("[%s:%d]", thread.getName(), thread.getId()), throwable);
        logger.error(ExceptionUtils.getStackTrace(throwable));
    }
}
