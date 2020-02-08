package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class ResponseTest {

    @Test
    public void testModelJson() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("key", "Public key");
        Response response = Response.ok().entity(map).build();
        assertEquals("{\"key\":\"Public key\"}", response.json());

        response = Response.ok().entity("{\"key\":\"Public key\"}").build();
        assertEquals("{\"key\":\"Public key\"}", response.json());
    }

    @Test
    public void testEmptyTargetJson() throws Exception {
        Map<String, String> map = new HashMap<>();
        Response response = Response.ok().entity(map).build();
        assertEquals("{}", response.json());

        response = Response.ok().entity(null).build();
        assertEquals("", response.json());
    }

    @Test
    public void testResponseHttpHeader() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("key", "Public key");
        Response.Builder builder = Response.ok().entity(map).cache(86400L);
        assertEquals("{\"key\":\"Public key\"}", builder.json());

        Headers headers = new Headers();
        builder.join(headers);
        assertEquals(3, headers.size());
        assertEquals("Server",  headers.keySet().toArray(new String[3])[0]);
        assertEquals("Content-type",  headers.keySet().toArray(new String[3])[1]);
        assertEquals("Cache-control",  headers.keySet().toArray(new String[3])[2]);
        List<String> values = headers.get("Cache-Control");
        assertEquals(1, values.size());
        assertEquals("tiny/1.0.0", headers.getFirst("Server"));
        assertEquals("application/json; charset=utf-8", headers.getFirst("Content-type"));
        assertEquals("max-age=86400", headers.getFirst("Cache-control"));
        assertEquals("max-age=86400", values.get(0));
    }

    @Test
    public void testCustomStatusSend() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("key", "Public key");
        Response response = Response.status(HttpURLConnection.HTTP_CREATED)
                .entity(map)
                .cookie("cookie1=12345", "cookie2=abcdef")
                .cache(86400L)
                .build();
        long len = response.json().length();
        Headers headers = new Headers();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        HttpExchange httpExchange = mock(HttpExchange.class);

        when(httpExchange.getResponseHeaders())
            .thenReturn(headers);

        doNothing().when(httpExchange).sendResponseHeaders(eq(201), eq(len));
        when(httpExchange.getResponseBody())
            .thenReturn(out);

        response.send(httpExchange);

        verify(httpExchange).sendResponseHeaders(eq(201), eq(len));
        assertEquals(4, headers.size());
        List<String> values = headers.get("Cache-Control");
        assertEquals(1, values.size());
        assertEquals("application/json; charset=utf-8", headers.getFirst("Content-type"));
        assertEquals("max-age=86400", headers.getFirst("Cache-control"));
        assertEquals("max-age=86400", values.get(0));
        assertEquals("cookie1=12345; cookie2=abcdef", headers.getFirst("Set-cookie"));
    }
}
