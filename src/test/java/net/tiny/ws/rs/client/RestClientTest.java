package net.tiny.ws.rs.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.ws.AccessLogger;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.ParameterFilter;
import net.tiny.ws.SnapFilter;
import net.tiny.ws.VoidHttpHandler;
import net.tiny.ws.WebServiceHandler;
import net.tiny.ws.rs.RestApplication;
import net.tiny.ws.rs.RestfulHttpHandler;

public class RestClientTest {

    static int port;
    static EmbeddedServer server;

    @BeforeAll
    public static void setUp() throws Exception {
        AccessLogger logger = new AccessLogger();
        ParameterFilter parameter = new ParameterFilter();
        SnapFilter snap = new SnapFilter();
        RestApplication application = new RestApplication();
        application.setPattern("net.tiny.ws.rs.test.*, !java.*, !javax.*, !com.sun.*, !org.junit.*,");
        RestfulHttpHandler rest = new RestfulHttpHandler();
        rest.setApplication(application);
        WebServiceHandler restful = rest.path("/api/v2/")
                .filters(Arrays.asList(parameter, logger, snap));

        WebServiceHandler health = new VoidHttpHandler()
                .path("/healthcheck")
                .filter(logger);

        server = new EmbeddedServer.Builder()
                .random()
                .handlers(Arrays.asList(restful, health))
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
    public static void tearDown() throws Exception {
        server.close();
    }

    @Test
    public void testRestClient() throws Exception {
        RestClient client = new RestClient.Builder()
                .build();


        RestClient.Request request = client.execute("http://localhost:" + port + "/api/v2/test/get/1234");

        RestClient.Response response = request.get(MediaType.APPLICATION_JSON);
        String body = response.getEntity();
        assertTrue(body.startsWith("{\"Id is 1234\"}"));
        System.out.println(body);
        response.close();
    }

}
