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

        AccessLogger logger = new AccessLogger();
        ParameterFilter parameter = new ParameterFilter();
        SnapFilter snap = new SnapFilter();
        RestApplication application = new RestApplication();
        application.setPattern("net.tiny.ws.rs.*, !java.*, !javax.*, !com.sun.*, !org.junit.*,");
        RestfulHttpHandler rest = new RestfulHttpHandler();
        rest.setApplication(application);
        rest.setupRestServiceFactory();
        WebServiceHandler restful = rest.path("/v1/api")
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
    public void testRestfulGet() throws Exception {
        String userAgent = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)";
        URL url = new URL("http://localhost:" + port +"/v1/api/test/get/12345");
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
