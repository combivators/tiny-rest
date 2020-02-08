package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tiny.ws.rs.test.TestApiService;

import java.util.HashMap;
import java.util.Map;

public class RestServiceFactoryTest {

    @BeforeEach
    public void setUp() throws Exception {
        System.setProperty("javax.ws.rs.scan.packages.include", "net.tiny.ws.rs.test.*");
        System.setProperty("javax.ws.rs.scan.packages.exclude", "java.*, com.sun.*");
        System.setProperty("javax.ws.rs.logging.level", "info");
    }

    @AfterEach
    public void tearDown() throws Exception {
        System.getProperties().remove("javax.ws.rs.scan.packages.include");
        System.getProperties().remove("javax.ws.rs.scan.packages.exclude");
        System.getProperties().remove("javax.ws.rs.logging.level");
    }

    @Test
    public void testFecthRestService() throws Exception {
        final RestApplication application = new RestApplication();
        application.setScan(".*/classes/, .*/test-classes/, .*/tiny-.*[.]jar,");

        RestServiceLocator context = new RestServiceLocator();
        context.bind("application", application, true);

        RestServiceFactory factory = new RestServiceFactory("/api", context, null);

        Map<String, Object> args = new HashMap<>();
        RestServiceHandler handler = factory.getRestServiceHandler("/api/v1/add/123/456", "GET", args);
        assertNotNull(handler);
        assertTrue(handler instanceof MethodPattern);
        assertEquals(2, args.size());

        args = new HashMap<>();
        handler = factory.getRestServiceHandler("/api/v2/test/get/123", "GET", args);
        assertNotNull(handler);
        assertTrue(handler instanceof MethodPattern);


        Object target = handler.getTarget();
        assertNotNull(target);
        assertTrue(target instanceof TestApiService);

        assertEquals(1, args.size());
        assertEquals("123", args.get("id"));

        Object ret = handler.invoke(new Object[] {"123"});
        assertNotNull(ret);
        assertEquals("Id is 123", ret);
    }

    @Test
    public void testNotFoundRestService() throws Exception {
        final RestApplication application = new RestApplication();
        application.setScan(".*/classes/, .*/test-classes/, .*/tiny-.*[.]jar,");
        RestServiceLocator context = new RestServiceLocator();
        context.bind("application", application, true);

        RestServiceFactory factory = new RestServiceFactory("/api", context, null);

        final Map<String, Object> args = new HashMap<>();
        RestServiceHandler handler = factory.getRestServiceHandler("/api/v1/test/unkonw/123", "GET", args);
        assertNull(handler);
    }
}
