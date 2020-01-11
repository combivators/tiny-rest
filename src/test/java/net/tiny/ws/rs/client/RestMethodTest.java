package net.tiny.ws.rs.client;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;


public class RestMethodTest {

    static class DummyBean {}

    static final String VER = "v1";
    interface Example01 {
        // Test URL: http://localhost:8080/v1/api/user/123
        @GET
        @Path("/" + VER + "/api/user/{id}")
        @Produces(value = MediaType.APPLICATION_JSON)
        public String getId(@PathParam("id")Integer id);
    }

    interface Example02 {
        // Test URL: http://localhost:8080/v1/api/user/123
        @POST
        //@Path("/" + VER + "/api/msg/{token}")
        @Produces(value = MediaType.APPLICATION_JSON)
        public void accept(@PathParam("token")String token, @BeanParam DummyBean bean);
    }

    @Test
    public void testRestMethod() throws Exception {
        Method method = Example01.class.getDeclaredMethod("getId", Integer.class);
        assertNotNull(method);

        RestMethod restMethod = new RestMethod(method, null);
        assertEquals("/v1", restMethod.getPath());
        assertEquals("/api/user/{id}", restMethod.getPattern());
        assertEquals(method, restMethod.getMethod());

        Object[] args = new Object[] {1234};
        String uri = restMethod.generateURI(args, "localhost", 8080, false);
        assertEquals("http://localhost:8080/v1/api/user/1234", uri);
    }

    @Test
    public void testStaticMethod() throws Exception {
        Map<String, String> args = RestMethod.parseQuery("user=hoge&type=guest");
        assertEquals(2, args.size());
        assertEquals("hoge", args.get("user"));
        assertEquals("guest", args.get("type"));

        Method method = Example01.class.getDeclaredMethod("getId", Integer.class);
        List<String> keys = RestMethod.getParameterKeys(method);
        assertEquals(1, keys.size());
        assertEquals("id", keys.get(0));


        assertEquals("from", RestMethod.matchQueryParameterKey("{from=\\d+}"));
        assertEquals("to", RestMethod.matchQueryParameterKey("{to:\\d+}"));
        assertEquals("id", RestMethod.matchQueryParameterKey("{id}"));
        assertNull(RestMethod.matchQueryParameterKey("type=guest"));
        assertNull(RestMethod.matchQueryParameterKey("type"));

        List<String> names = RestMethod.getQueryParameterKeys("/v1/api/user/{id}");
        assertEquals(1, names.size());
        assertEquals("id", names.get(0));

        names = RestMethod.getQueryParameterKeys("/v1/api/s?{from=\\d+}&{to:\\d+}&{order}");
        assertEquals(3, names.size());
        assertEquals("from", names.get(0));
        assertEquals("to", names.get(1));
        assertEquals("order", names.get(2));

        names = RestMethod.getQueryParameterKeys("/v1/api/s?from={from}&to:{to}&{order}");
        assertEquals(3, names.size());
        assertEquals("from", names.get(0));
        assertEquals("to", names.get(1));
        assertEquals("order", names.get(2));

    }

    @Test
    public void testBingValue() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", 123);
        String query = RestMethod.bingValue(params, "/api/user/{id}");
        assertEquals("/api/user/123", query);

        params.clear();
        params.put("from", "2000122531");
        params.put("to", "2012010203");
        params.put("order", "true");
        query = RestMethod.bingValue(params, "/api/s?from={from}&to:{to}&{order}");
        assertEquals("/api/s?from=2000122531&to:2012010203&true", query);

        query = RestMethod.bingValue(params, "/api/s?{from=\\d+}&{to:\\d+}&{order}");
        assertEquals("/api/s?2000122531&2012010203&true", query);
    }

    @Test
    public void testRestMethodWithoutPath() throws Exception {
        Method method = Example02.class.getDeclaredMethod("accept", String.class, DummyBean.class);
        assertNotNull(method);

        RestMethod restMethod = new RestMethod(method, "/" + VER + "/api/msg/{token}");
        assertEquals("/v1", restMethod.getPath());
        assertEquals("/api/msg/{token}", restMethod.getPattern());
        assertEquals(method, restMethod.getMethod());

        Object[] args = new Object[] {"1234", new DummyBean()};
        String uri = restMethod.generateURI(args, "localhost", 8080, false);
        assertEquals("http://localhost:8080/v1/api/msg/1234", uri);
    }
}
