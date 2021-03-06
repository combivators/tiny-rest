package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.LogManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.ws.AccessLogger;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.ParameterFilter;
import net.tiny.ws.SnapFilter;
import net.tiny.ws.VoidHttpHandler;
import net.tiny.ws.WebServiceHandler;


public class RestfulServerTest {

    static int port;
    static EmbeddedServer server;

    @BeforeAll
    public static void setUp() throws Exception {
        LogManager.getLogManager().readConfiguration(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));

        RestApplication application = new RestApplication();
        application.setPattern("net.tiny.ws.rs.test.*, !java.*, !javax.*, !com.sun.*, !org.junit.*,");
        application.setScan(".*/classes/, .*/test-classes/, .*/tiny-.*[.]jar,");

        RestServiceLocator context = new RestServiceLocator();
        context.bind("application", application, true);

        AccessLogger logger = new AccessLogger();
        ParameterFilter parameter = new ParameterFilter();
        SnapFilter snap = new SnapFilter();

        WebServiceHandler health = new VoidHttpHandler()
                .path("/healthcheck")
                .filter(logger);

        RestServiceLocator.RestServiceMonitor listener = new RestServiceLocator.RestServiceMonitor();


        RestfulHttpHandler restApi = new RestfulHttpHandler();
        restApi.path("/api");
        restApi.setListener(listener);
        restApi.setContext(context);
        restApi.setupRestServiceFactory();

        WebServiceHandler api = restApi.filters(Arrays.asList(parameter, logger, snap));


        RestfulHttpHandler restUi = new RestfulHttpHandler();
        restUi.path("/ui");
        restUi.setListener(listener);
        restUi.setContext(context);
        restUi.setupRestServiceFactory();

        WebServiceHandler ui = restUi.filters(Arrays.asList(parameter, logger, snap));


        server = new EmbeddedServer.Builder()
                .random()
                .handlers(Arrays.asList(api, ui, health))
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
    public void testRestfulGet() throws Exception {
        String userAgent = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)";
        URL url = new URL("http://localhost:" + port +"/api/v2/test/get/12345");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Keep-Alive", "header");
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setInstanceFollowRedirects(true);

        assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            assertEquals("application/json; charset=utf-8", connection.getHeaderField("Content-Type"));
            int size = connection.getContentLength();
            BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
            byte[] contents = getContent(size, bis);
            bis.close();
            assertEquals(size, contents.length);
            System.out.println(new String(contents));
        }else{
            fail("HTTP Status : " + connection.getResponseCode());
        }


        connection.disconnect();
    }

    byte[] getContent(int contentLength, InputStream in) throws IOException {
        ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
        byte readBuf[] = new byte[contentLength];
        int readLen = 0;
        while((readLen = in.read(readBuf)) > 0 ) {
            contentBuffer.write(readBuf, 0, readLen);
            contentBuffer.flush();
        }
        return contentBuffer.toByteArray();
    }

}
