package config;

import java.io.PrintStream;

public class stdErr {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(stdErr.class);

    public static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                realPrintStream.print(string);
                logger.error(string);
            }
        };
    }
}
