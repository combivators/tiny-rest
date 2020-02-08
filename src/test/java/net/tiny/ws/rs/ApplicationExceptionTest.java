package net.tiny.ws.rs;

import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ApplicationExceptionTest {

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager()
            .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }


    @Test
    public void testExceptionCause() throws Exception {
        Throwable ex = new IllegalArgumentException("Argument error");
        ApplicationException err = new ApplicationException(ex, HttpURLConnection.HTTP_INTERNAL_ERROR);
        Logger logger = Logger.getLogger("testExceptionCause");
        logger.log(Level.WARNING, err.getMessage(), err.getCause());
    }
}
