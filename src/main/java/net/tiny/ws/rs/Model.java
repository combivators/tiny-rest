package net.tiny.ws.rs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.JsonParser;
import net.tiny.ws.HttpDateFormat;

public class Model {

    private final Object target;
    private int status = HttpURLConnection.HTTP_OK;

    private Map<String, List<String>> headers = new LinkedHashMap<>();

    public Model(Object target) {
        this.target = target;
    }

    public String json() {
        if (null != target) {
            if (target instanceof String) {
                return (String)target;
            } else {
                return JsonParser.marshal(target);
            }
        }
        return "";
    }

    public Object target() {
        return target;
    }

    public Model cookie(String... cookies) {
        StringBuilder sb = new StringBuilder();
        for (String c : cookies) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(c);
        }
        return header("Set-Cookie", sb.toString());
    }

    public Model cache(long maxAge) {
        if (maxAge > 0L) {
            return header("Cache-Control", "max-age=" + maxAge);
        } else {
            return header("Cache-Control", "no-cache");
        }
    }

    public Model lastModified(Date date) {
        return header("Last-Modified", HttpDateFormat.format(date));
    }

    public Model header(String name, String value) {
        List<String> values = headers.get(name);
        if (null == values) {
            values = new ArrayList<>();
            values.add(value);
            headers.put(name, values);
        } else if (!values.contains(value)){
            values.add(value);
        }
        return this;
    }

    public Model join(Headers headers) {
        for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
            for (String value : entry.getValue()) {
                headers.add(entry.getKey(), value);
            }
        }
        headers.add("Content-Type", "application/json; charset=utf-8");
        return this;
    }

    public Model status(int code) {
        status = code;
        return this;
    }

    public void send(HttpExchange he) throws IOException {
        final String response = join(he.getResponseHeaders()).json();
        final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(status, rawResponse.length);
        he.getResponseBody().write(rawResponse);
    }
}
