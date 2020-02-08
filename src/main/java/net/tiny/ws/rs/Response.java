package net.tiny.ws.rs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.JsonParser;
import net.tiny.ws.HttpDateFormat;

public final class Response {

    private final static String SERVER_NAME = "tiny/1.0.0";

    private final Builder builder;

    private Response(Builder builder) {
        this.builder= builder;
    }

    public String json() {
        return builder.json();
    }

    public void send(HttpExchange he) throws IOException {
        final String response = builder.join(he.getResponseHeaders()).json();

        final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(builder.status, rawResponse.length);
        he.getResponseBody().write(rawResponse);
    }

    public static Builder ok() {
        return new Builder();
    }

    public static Builder status(int c) {
        return new Builder().status(c);
    }

    public static class Builder {
        int status = HttpURLConnection.HTTP_OK;
        Object entity;
        Map<String, List<String>> headers = new LinkedHashMap<>();

        public Builder() {
            headers.put("Server", Arrays.asList(SERVER_NAME));
        }

        public Builder status(int c) {
            status = c;
            return this;
        }

        public Builder entity(Object e) {
            entity = e;
            return this;
        }

        public Builder server(String s) {
            headers.put("Server", Arrays.asList(s));
            return this;
        }

        public Builder cookie(String... cookies) {
            StringBuilder sb = new StringBuilder();
            for (String c : cookies) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(c);
            }
            return header("Set-Cookie", sb.toString());
        }

        public Builder cache(long maxAge) {
            if (maxAge > 0L) {
                return header("Cache-Control", "max-age=" + maxAge);
            } else {
                return header("Cache-Control", "no-cache");
            }
        }

        public Builder lastModified(Date date) {
            return header("Last-Modified", HttpDateFormat.format(date));
        }

        public Builder header(String name, String value) {
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

        public Builder join(Headers headers) {
            for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
                for (String value : entry.getValue()) {
                    headers.add(entry.getKey(), value);
                }
            }
            headers.add("Content-Type", "application/json; charset=utf-8");
            return this;
        }

        public String json() {
            if (null != entity) {
                if (entity instanceof String) {
                    return (String)entity;
                } else {
                    return JsonParser.marshal(entity);
                }
            }
            return "";
        }

        public Response build() {
            return new Response(this);
        }
    }
}
