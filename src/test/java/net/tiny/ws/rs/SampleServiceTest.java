package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.LogManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.ws.AccessLogger;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.SnapFilter;
import net.tiny.ws.WebServiceHandler;
import net.tiny.ws.rs.client.RestClient;

public class SampleServiceTest {

    static String BROWSER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)";
    static int port;
    static EmbeddedServer server;

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager().readConfiguration(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));

        AccessLogger logger = new AccessLogger();
        SnapFilter snap = new SnapFilter();
        //ParameterFilter parameter = new ParameterFilter();

        WebServiceHandler simple = new SimpleRestHttpHandler()
                .path("/opt")
                .filters(Arrays.asList(logger, snap));

        RestApplication application = new RestApplication();
        application.setPattern("net.tiny.*, !java.*, !javax.*, !com.sun.*, !org.junit.*,");
        RestfulHttpHandler rest = new RestfulHttpHandler();
        rest.setApplication(application);
        rest.setListener(new RestServiceLocator.RestServiceMonitor());
        rest.setupRestServiceFactory();

        WebServiceHandler restful = rest.path("/v1/api")
                .filters(Arrays.asList(logger, snap));

        server = new EmbeddedServer.Builder()
                .random()
                .handlers(Arrays.asList(restful, simple))
                .build();
        port = server.port();
        server.listen(callback -> {
            if(callback.success()) {
                System.out.println("Server listen on port: " + port);
            } else {
                callback.cause().printStackTrace();
            }
        });
    }

    @AfterAll
    public static void afterAll() throws Exception {
        server.close();
        server.awaitTermination();
    }

    @Test
    public void testGetEntity() throws Exception {
        RestClient client = new RestClient.Builder()
                .userAgent(BROWSER_AGENT)
                .build();

        // Test GET
        RestClient.Response response = client.doGet(new URL("http://localhost:" + port +"/v1/api/add/12.3/4.56"));
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        assertTrue(response.hasEntity());
        String body = response.getEntity();
        assertNotNull(body);
        assertEquals("{\" 12.300 + 4.560 = 16.860\"}\n", body);


        response = client.doGet(new URL("http://localhost:" + port +"/v1/api/plus/12.3/4.56"));
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_NOT_FOUND);
        response.close();

        response = client.doGet(new URL("http://localhost:" + port +"/v1/api/query?from=10&to=900&order=age"));
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        assertTrue(response.hasEntity());
        body = response.getEntity();
        assertNotNull(body);
        assertEquals("{\"query is called, from : 10, to : 900, order by age\"}\n", body);
        response.close();

    }

    @Test
    public void testPostEntity() throws Exception {
        RestClient client = new RestClient.Builder()
                .userAgent(BROWSER_AGENT)
                .build();

        // Test GET
        URL url = new URL("http://localhost:" + port +"/v1/api/ask/123");
        SampleService.DummyEmail mail = new SampleService.DummyEmail();
        mail.email = "user@example.com";

        RestClient.Response response = client.doPost(url, mail);
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        assertTrue(response.hasEntity());
        SampleService.DummyUser user = response.readEntity(SampleService.DummyUser.class);
        assertNotNull(user);
        assertEquals("user", user.name);
        assertEquals("example.com", user.domain);

        response.close();
    }

}
