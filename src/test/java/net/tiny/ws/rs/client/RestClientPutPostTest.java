package net.tiny.ws.rs.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.LogManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tiny.ws.AccessLogger;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.SnapFilter;
import net.tiny.ws.WebServiceHandler;

public class RestClientPutPostTest {

    static String BROWSER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)";
    static int port;
    static EmbeddedServer server;

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager().readConfiguration(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }

    @BeforeEach
    public void setUp() throws Exception {
        AccessLogger logger = new AccessLogger();
        SnapFilter snap = new SnapFilter();

        WebServiceHandler handler = new PostPutOptionHandler()
                .filters(Arrays.asList(logger, snap));

        server = new EmbeddedServer.Builder()
                .random()
                .handler("/opt", handler)
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

    @AfterEach
    public void tearDown() throws Exception {
        server.close();
        server.awaitTermination();
    }

    @Test
    public void testPutPostEntity() throws Exception {
        RestClient client = new RestClient.Builder()
                .userAgent(BROWSER_AGENT)
                .build();

        URL url = new URL("http://localhost:" + port +"/opt");
        PostPutOptionHandler.DummyEmail mail = new PostPutOptionHandler.DummyEmail();
        mail.email = "user@example.com";

        // Test PUT
        RestClient.Response response = client.doPut(url, mail);
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        assertTrue(response.hasEntity());
        PostPutOptionHandler.DummyEmail ret = response.readEntity(PostPutOptionHandler.DummyEmail.class);
        assertNotNull(ret);
        assertEquals("user@example.com", ret.email);

        response.close();

        // Test POST
        response = client.doPost(url, mail);
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        assertTrue(response.hasEntity());
        PostPutOptionHandler.DummyUser user = response.readEntity(PostPutOptionHandler.DummyUser.class);
        assertNotNull(ret);
        assertEquals("user", user.name);
        assertEquals("example.com", user.domain);
        response.close();
    }
}
