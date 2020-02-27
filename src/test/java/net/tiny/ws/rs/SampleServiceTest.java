package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.ws.AccessLogger;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.SnapFilter;
import net.tiny.ws.WebServiceHandler;
import net.tiny.ws.rs.client.RestClient;
import net.tiny.ws.rs.test.SampleApiService;

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
        application.setScan(".*/classes/, .*/test-classes/, .*/tiny-.*[.]jar,");
        RestServiceLocator context = new RestServiceLocator();
        context.bind("application", application, true);

        RestfulHttpHandler rest = new RestfulHttpHandler();
        rest.path("/api/v1");
        rest.setListener(new RestServiceLocator.RestServiceMonitor());
        rest.setContext(context);
        rest.setupRestServiceFactory();

        WebServiceHandler restful = rest.filters(Arrays.asList(logger, snap));

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
        RestClient.Response response = client.doGet(new URL("http://localhost:" + port +"/api/v1/add/12.3/4.56"));
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        assertTrue(response.hasEntity());
        String body = response.getEntity();
        assertNotNull(body);
        assertEquals("{\" 12.300 + 4.560 = 16.860\"}", body);


        response = client.doGet(new URL("http://localhost:" + port +"/api/v1/plus/12.3/4.56"));
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_NOT_FOUND);
        response.close();

        response = client.doGet(new URL("http://localhost:" + port +"/api/v1/query?from=10&to=900&order=age"));
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        assertTrue(response.hasEntity());
        body = response.getEntity();
        assertNotNull(body);
        assertEquals("{\"query is called, from : 10, to : 900, order by age\"}", body);
        response.close();

    }

    @Test
    public void testQuery() throws Exception {
        RestClient client = new RestClient.Builder()
                .userAgent(BROWSER_AGENT)
                .build();

        // Test GET
        //RestClient.Response response = client.doGet(new URL("http://localhost:" + port +"/api/v1/search?x=139.809692&y=35.736832&n=10"));
        RestClient.Response response = client.doGet(new URL("http://localhost:" + port +"/api/v1/search?x=139.809692&y=35.736832&n=10"));
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        assertTrue(response.hasEntity());
        String body = response.getEntity();
        assertNotNull(body);
        System.out.println(body);
        //assertEquals("{\"query is called, from : 10, to : 900, order by age\"}", body);
        response.close();

    }


    @Test
    public void testPostEntity() throws Exception {
        RestClient client = new RestClient.Builder()
                .userAgent(BROWSER_AGENT)
                .build();

        // Test GET
        URL url = new URL("http://localhost:" + port +"/api/v1/ask/123");
        SampleApiService.DummyEmail mail = new SampleApiService.DummyEmail();
        mail.email = "user@example.com";

        RestClient.Response response = client.doPost(url, mail);
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        assertTrue(response.hasEntity());
        SampleApiService.DummyUser user = response.readEntity(SampleApiService.DummyUser.class);
        assertNotNull(user);
        assertEquals("user", user.name);
        assertEquals("example.com", user.domain);

        response.close();
    }

    @Test
    public void testCookie() throws Exception {
        RestClient client = new RestClient.Builder()
                .userAgent(BROWSER_AGENT)
                .build();

        // Test Cookie
        RestClient.Request request = client.execute(new URL("http://localhost:" + port +"/api/v1/cookie"))
                .cookie("cookie1=12345; cookie2=abcdef");
        RestClient.Response response = request.get();
        assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        List<HttpCookie> cookies = response.getCookies();

        assertEquals(1, cookies.size());
        HttpCookie cookie = cookies.get(0);
        assertEquals("12345abcdef", cookie.getValue());
        assertEquals("12345abcdef", response.getCookie("authToken"));
        assertTrue(response.hasEntity());
        String body = response.getEntity();
        assertNotNull(body);
        assertEquals("{\"token\":\"1234567890abcdef\"}", body);
        response.close();
    }

}
