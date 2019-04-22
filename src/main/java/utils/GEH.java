package utils;

public class GEH implements Thread.UncaughtExceptionHandler {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GEH.class);

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        logger.error(String.format("[%s:%d]", thread.getName(), thread.getId()), throwable);
    }
}
