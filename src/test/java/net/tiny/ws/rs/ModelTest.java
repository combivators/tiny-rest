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

public class ModelTest {

    @Test
    public void testModelJson() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("key", "Public key");
        Model model = new Model(map);
        assertEquals("{\"key\":\"Public key\"}", model.json());

        model = new Model("{\"key\":\"Public key\"}");
        assertEquals("{\"key\":\"Public key\"}", model.json());
    }

    @Test
    public void testEmptyTargetJson() throws Exception {
        Map<String, String> map = new HashMap<>();
        Model model = new Model(map);
        assertEquals("{}", model.json());

        model = new Model(null);
        assertEquals("", model.json());
    }

    @Test
    public void testResponseHttpHeader() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("key", "Public key");
        Model model = new Model(map).cache(86400L);
        assertEquals("{\"key\":\"Public key\"}", model.json());

        Headers headers = new Headers();
        model.join(headers);
        assertEquals(2, headers.size());
        assertEquals("Content-type",  headers.keySet().toArray(new String[2])[0]);
        assertEquals("Cache-control",  headers.keySet().toArray(new String[2])[1]);
        List<String> values = headers.get("Cache-Control");
        assertEquals(1, values.size());
        assertEquals("application/json; charset=utf-8", headers.getFirst("Content-type"));
        assertEquals("max-age=86400", headers.getFirst("Cache-control"));
        assertEquals("max-age=86400", values.get(0));
    }

    @Test
    public void testCustomStatusSend() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("key", "Public key");
        Model model = new Model(map)
                .cookie("cookie1=12345", "cookie2=abcdef")
                .cache(86400L)
                .status(HttpURLConnection.HTTP_CREATED);
        long len = model.json().length();
        Headers headers = new Headers();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        HttpExchange httpExchange = mock(HttpExchange.class);

        when(httpExchange.getResponseHeaders())
            .thenReturn(headers);

        doNothing().when(httpExchange).sendResponseHeaders(eq(201), eq(len));
        when(httpExchange.getResponseBody())
            .thenReturn(out);

        model.send(httpExchange);

        verify(httpExchange).sendResponseHeaders(eq(201), eq(len));
        assertEquals(3, headers.size());
        List<String> values = headers.get("Cache-Control");
        assertEquals(1, values.size());
        assertEquals("application/json; charset=utf-8", headers.getFirst("Content-type"));
        assertEquals("max-age=86400", headers.getFirst("Cache-control"));
        assertEquals("max-age=86400", values.get(0));
        assertEquals("cookie1=12345; cookie2=abcdef", headers.getFirst("Set-cookie"));
    }
}
