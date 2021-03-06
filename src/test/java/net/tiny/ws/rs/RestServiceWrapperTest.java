package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;

import net.tiny.ws.rs.test.SampleApiService;

public class RestServiceWrapperTest {

    @Path("rest")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Example01 {

        @GET
        @Path("/add/{a}/{b}")
        public String add(@PathParam("a")double a, @PathParam("b")double b) {
            return String.format(" %1$.3f + %2$.3f = %3$.3f", a, b, (a+b));
        }

        @GET
        @Path("/login/{login: [a-z]*}")
        @Produces(MediaType.TEXT_PLAIN)
        public String login(@PathParam("login") String login) {
            return login;
        }

        @GET
        @Path("/login/{customerId : \\d+}")
        public String customer(@PathParam("customerId") Long id) {
            return id.toString();
        }

        @GET
        @Path("/private/{login: [a-z]*}")
        private String priv(@PathParam("login") String login) {
            return login;
        }

        @GET
        @Path("/protected/{login: [a-z]*}")
        protected String protec(@PathParam("login") String login) {
            return login;
        }

        @POST
        @Path("post")
        @Produces(MediaType.TEXT_HTML)
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String post(@FormParam("id") Long id,
                @FormParam("name") String name)  {
            return id + "- " + name;
        }
    }

    @Path("/")
    public static class Example02 {

        @GET
        @Path("api/v10/add/{a}/{b}")
        public String add(@PathParam("a")double a, @PathParam("b")double b) {
            return String.format(" %1$.3f + %2$.3f = %3$.3f", a, b, (a+b));
        }

        @GET
        @Path("ui/user/{id}/edit")
        @Produces(MediaType.TEXT_HTML)
        public String edit(@PathParam("id")int id) {
            return "<!DOCTYPE html><html><head><title>User Profile</title></head><body>User Profile Contents</body></html>";
        }

    }
/*
    @Test
    public void testCookieToString() throws Exception {
          Cookie cookie = new Cookie("name","andrew");
          assertEquals("name=\"andrew\"; $Version=1",cookie.toString());
          cookie=new Cookie("name","andrew","/exo","exo.com");
          assertEquals("name=\"andrew\"; $Version=1; $Path=/exo; $Domain=exo.com",cookie.toString());
    }
*/

    @Test
    public void testGetService() throws Exception {
        RestServiceLocator.RestServiceMonitor monitor = new RestServiceLocator.RestServiceMonitor();
        final RestServiceWrapper restService = new RestServiceWrapper(new Example01(), monitor);
        assertEquals("/rest", restService.getPath());
        assertTrue(restService.getMode() == Constants.Mode.instance);
        assertTrue(restService.getServiceClass().equals(Example01.class));

        System.out.println(restService.toString());
        assertEquals(0,  restService.hit("/rest/add/111/222", "GET").getHit());
        assertEquals(0,  restService.hit("/rest/login/123", "GET").getHit());
        assertEquals(0,  restService.hit("/rest/login/abc", "GET").getHit());
        assertEquals(-8, restService.hit("/rest/xyz/abc/222", "GET").getHit());
        assertEquals(15,  restService.hit("/rest/aaa/abc", "GET").getHit());
        assertEquals(15,  restService.hit("/rest/add/111/222", "POST").getHit());

        assertTrue(restService.matches("/rest"));
        assertFalse(restService.matches("/api"));
    }

    @Test
    public void testGetRestApiWithUi() throws Exception {
        RestServiceLocator.RestServiceMonitor monitor = new RestServiceLocator.RestServiceMonitor();
        final RestServiceWrapper restService = new RestServiceWrapper(new Example02(), monitor);
        assertEquals("/", restService.getPath());
        assertTrue(restService.getMode() == Constants.Mode.instance);
        assertTrue(restService.getServiceClass().equals(Example02.class));

        System.out.println(restService.toString());
        assertEquals(0,  restService.hit("/api/v10/add/111/222", "GET").getHit());
        assertEquals(0,  restService.hit("/ui/user/123/edit", "GET").getHit());
        assertEquals(20,  restService.hit("/api/v1/add/123/456", "GET").getHit());
        assertEquals(20,  restService.hit("/ui/admin/123/edit", "GET").getHit());

        assertTrue(restService.matches("/api"));
        assertTrue(restService.matches("/api/v1"));
        assertTrue(restService.matches("/ui"));

        assertFalse(restService.matches("/v1"));
        assertFalse(restService.matches("/v2"));
    }

    @Test
    public void testQueryParamWithoutPattern() throws Exception {
        RestServiceLocator.RestServiceMonitor monitor = new RestServiceLocator.RestServiceMonitor();
        final RestServiceWrapper restService = new RestServiceWrapper(new SampleApiService(), monitor);
        assertEquals("/api/v1", restService.getPath());
        assertTrue(restService.getMode() == Constants.Mode.instance);
        assertTrue(restService.getServiceClass().equals(SampleApiService.class));
        System.out.println(restService.toString());

        Map<String, Object> args = new HashMap<String, Object>();
        Hitting<?> hitting = restService.hit("/api/v1/query?from=10&to=999&order=%5BItem1%2C+Item2%2C+Item3%5D", "GET", args);

        assertEquals(0, hitting.getHit());
        assertEquals(3, args.size());
        assertEquals("10", args.get("from"));
        assertEquals("999", args.get("to"));
        assertEquals("[Item1, Item2, Item3]", args.get("order"));
    }
}
