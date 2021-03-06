package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import net.tiny.ws.rs.test.SampleApiService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

public class MethodPatternTest {

    class Example01 {
        @GET
        @Path("/add/{a}/{b}")
        public String add(@PathParam("a")double a, @PathParam("b")double b) {
            return String.format(" %1$.3f + %2$.3f = %3$.3f", a, b, (a+b));
        }
    }

    @Test
    public void testValidateExample01Patterns() throws Exception {
        Method[] methods = Example01.class.getDeclaredMethods();
        //Test Example01#add(double, double)
        Method method = methods[0];
        assertNotNull(method);
        assertEquals("add", method.getName());
        MethodPattern methodPattern = new MethodPattern("rest", "/add/{a}/{b}", "GET", MediaType.APPLICATION_XML, Example01.class, null, method);

        assertFalse(methodPattern.validatePattern("", "POST"));
        assertFalse(methodPattern.validatePattern("", "GET"));
        assertFalse(methodPattern.validatePattern("rst/add/12/23", "GET"));
        assertFalse(methodPattern.validatePattern("rest/ad/12/23", "GET"));
        assertFalse(methodPattern.validatePattern("rest/add/12/23/45", "GET"));

        assertTrue(methodPattern.validatePattern("rest/add/12", "GET"));
        assertTrue(methodPattern.validatePattern("rest/add/12/23", "GET"));
        assertFalse(methodPattern.validatePattern("rest/addd/12/23/45", "GET"));
    }

    @Test
    public void testValidateExample01ValidatePattern() throws Exception {
        Method[] methods = Example01.class.getDeclaredMethods();
        Method method = methods[0];
        assertNotNull(method);
        assertEquals("add", method.getName());
        MethodPattern methodPattern = new MethodPattern("rest", "/add/{a}/{b}", "GET", MediaType.APPLICATION_XML, Example01.class, null, method);

        final Map<String, Object> args = new LinkedHashMap<String, Object>();
        assertTrue(methodPattern.validatePattern("rest/add/12/23", "GET", args));
        assertEquals(2, args.size());
        assertEquals("12", args.get("a"));
        assertEquals("23", args.get("b"));

        args.clear();
        assertTrue(methodPattern.validatePattern("rest/add/12", "GET", args));
        assertEquals(1, args.size());
        assertEquals("12", args.get("a"));
    }

    class Example02 {
        @GET
        @Path("login/{login: [a-z]*}")
        public String login(@PathParam("login") String login) {
            return login;
        }

        @GET
        @Path("ui/svg/{fas}/{icon}/{color}/{bg}")
        @Produces(value = MediaType.APPLICATION_SVG_XML)
        public String image(@PathParam("fas")String fas, @PathParam("icon")String icon, @PathParam("color")String color, @PathParam("bg")String background) {
            return "<svg></svg>";
        }
    }

    @Test
    public void testValidateExample02Patterns() throws Exception {
        Method[] methods = Example02.class.getDeclaredMethods();
        Method method = Example02.class.getMethod("login", String.class);
        assertNotNull(method);
        assertEquals("login", method.getName());
        MethodPattern methodPattern = new MethodPattern("rest", "/login/{login: [a-z]*}", "GET", MediaType.APPLICATION_XML, Example02.class, null, method);

        assertFalse(methodPattern.validatePattern("rst/login/abc", "GET"));
        assertFalse(methodPattern.validatePattern("rest/log/abc", "GET"));
        assertFalse(methodPattern.validatePattern("rest/login/abc/xyz", "GET"));
        assertFalse(methodPattern.validatePattern("rest/login/1234", "GET"));

        assertTrue(methodPattern.validatePattern("rest/login/abc", "GET"));


        method = Example02.class.getMethod("image", String.class, String.class, String.class, String.class);
        assertNotNull(method);
        assertEquals("image", method.getName());
        methodPattern = new MethodPattern("rest", "/ui/svg/{fas}/{icon}/{color}/{bg}", "GET", MediaType.APPLICATION_SVG_XML, Example02.class, null, method);

        assertTrue(methodPattern.validatePattern("rest/ui/svg/fas/fa-user-circle/blue/gra", "GET"));
    }

    @Test
    public void testHittingExample02Patterns() throws Exception {
        Method[] methods = Example02.class.getDeclaredMethods();
        Method method = Example02.class.getMethod("login", String.class);
        assertNotNull(method);
        assertEquals("login", method.getName());
        MethodPattern methodPattern = new MethodPattern("/customer", "/login/{login: [a-z]*}", "GET", MediaType.APPLICATION_XML, Example02.class, null, method);

        Map<String, Object> args = new HashMap<String, Object>();
        int hit = methodPattern.hit("/customer/login/john", "GET", args);
        assertEquals(0, hit);
        assertEquals(1, args.size());
        assertEquals("john", args.get("login"));
    }


    @Test
    public void testValidateExample02ValidatePattern() throws Exception {
        Method[] methods = Example02.class.getDeclaredMethods();
        assertEquals(2, methods.length);
        Method method = Example02.class.getMethod("login", String.class);
        assertNotNull(method);
        assertEquals("login", method.getName());
        MethodPattern methodPattern = new MethodPattern("rest", "/login/{login: [a-z]*}", "GET", MediaType.APPLICATION_XML, Example02.class, null, method);

        final Map<String, Object> args = new LinkedHashMap<String, Object>();
        assertTrue(methodPattern.validatePattern("rest/login/abc", "GET", args));
        assertEquals(1, args.size());
        assertEquals("abc", args.get("login"));
    }


    class Example03 {
        @GET
        @Path("{customerId : \\d+}")
        public String customer(@PathParam("customerId") Long id) {
            return id.toString();
        }
    }

    @Test
    public void testValidateExample03Patterns() throws Exception {
        Method[] methods = Example03.class.getDeclaredMethods();
        Method method = methods[0];
        assertNotNull(method);
        assertEquals("customer", method.getName());
        MethodPattern methodPattern = new MethodPattern("rest", "/customer/{customerId : \\d+}", "GET", MediaType.APPLICATION_XML, Example03.class, null, method);
        assertFalse(methodPattern.validatePattern("rst/customer/abc", "GET"));
        assertFalse(methodPattern.validatePattern("rest/custom/abc", "GET"));
        assertFalse(methodPattern.validatePattern("rest/customer/12/34", "GET"));
        assertFalse(methodPattern.validatePattern("rest/customer/abc", "GET"));

        final Map<String, Object> args = new LinkedHashMap<String, Object>();
        assertTrue(methodPattern.validatePattern("rest/customer/1234", "GET", args));
        assertEquals(1, args.size());
        assertEquals("1234", args.get("customerId"));
    }

    class Example04 {
        // Test URL: http://localhost:8080/eac-rest/customer?zip=1232&city=bejing
        @GET
        public String zip(@QueryParam("zip") Long zip, @QueryParam("city") String city, @QueryParam("dumy") String dumy) {
            return zip.toString();
        }
    }

    @Test
    public void testValidateExample04Patterns() throws Exception {
        Method[] methods = Example04.class.getDeclaredMethods();
        Method method = methods[0];
        assertNotNull(method);
        assertEquals("zip", method.getName());
        MethodPattern methodPattern = new MethodPattern("rest", "/customer?{zip=\\d+}&{city=[a-z]*}&{dumy}", "GET", MediaType.APPLICATION_XML, Example04.class, null, method);

        assertFalse(methodPattern.validatePattern("rest/customer;zip=1234", "GET"));
        assertFalse(methodPattern.validatePattern("rest/customer/zip=1234", "GET"));
        assertFalse(methodPattern.validatePattern("rest/customer?zip=abc", "GET"));
        assertFalse(methodPattern.validatePattern("rest/customer?zip=1232&city=bejing&abc=xyz", "GET"));
        assertFalse(methodPattern.validatePattern("rest/customer?zip=1232&city=bejing&dumy=a12&abc=xyz", "GET"));

        assertTrue(methodPattern.validatePattern("rest/customer?zip=1232", "GET"));
        assertTrue(methodPattern.validatePattern("rest/customer?zip=1232&city=bejing", "GET"));
        assertTrue(methodPattern.validatePattern("rest/customer?zip=1232&city=bejing&dumy=a12", "GET"));

        Map<String, Object> args = new HashMap<String, Object>();
        int hit = methodPattern.hit("rest/customer?zip=1232&city=bejing&dumy=a12", "GET", args);
        System.out.println(hit + "  " + args.toString());
    }

    class Example05 {
        // ex) http://localhost:8080/rest/matrix_param/HOGE;attr=fuga
        @GET
        @Path("/matrix_param;{msg};{attr}")
        public String matrixParam(@MatrixParam("msg") String message, @MatrixParam("attr") String attr) {
            return "message:" + message + "  attr:" + attr;
        }
    }

    @Test
    public void testValidateExample05Patterns() throws Exception {
        Method[] methods = Example05.class.getDeclaredMethods();
        Method method = methods[0];
        assertNotNull(method);
        assertEquals("matrixParam", method.getName());
        MethodPattern methodPattern = new MethodPattern("rest", "/matrix_param;{msg};{attr}", "GET", MediaType.APPLICATION_XML, Example05.class, null, method);

        //assertFalse(methodPattern.validatePattern("rest/matrix_param1/HOGE;attr=fuga", "GET"));

        final Map<String, Object> args = new LinkedHashMap<String, Object>();
        assertTrue(methodPattern.validatePattern("rest/matrix_param;msg=HOGE;attr=fuga", "GET", args));
        assertEquals("HOGE", args.get("msg"));
        assertEquals("fuga", args.get("attr"));
        assertEquals(2, args.size());
    }

    @Test
    public void testValidateExample04ValidatePattern() throws Exception {
        Method[] methods = Example04.class.getDeclaredMethods();
        Method method = methods[0];
        assertNotNull(method);
        assertEquals("zip", method.getName());
        MethodPattern methodPattern = new MethodPattern("rest", "/customer?{zip=\\d+}&{city=[a-z]*}&{dumy}", "GET", MediaType.APPLICATION_XML, Example04.class, null, method);


        final Map<String, Object> args = new LinkedHashMap<String, Object>();
        assertTrue(methodPattern.validatePattern("rest/customer?zip=1232", "GET", args));
        assertEquals(1, args.size());
        assertEquals("1232", args.get("zip"));

        args.clear();
        assertTrue(methodPattern.validatePattern("rest/customer?zip=1232&city=bejing&dumy=a12", "GET", args));
        assertEquals(3, args.size());
        assertEquals("1232", args.get("zip"));
        assertEquals("bejing", args.get("city"));
        assertEquals("a12", args.get("dumy"));
    }

    class Example06 {
        // Test URL: http://localhost:8080/eac-rest/divide.do
        @POST
        @Produces(MediaType.TEXT_HTML)
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public void divide(@FormParam("divisor")Double divisor,
                @FormParam("roundingMode")Double roundingMode) throws IOException {
        }
    }
    @Test
    public void testValidateExample06PostPatterns() throws Exception {
        Method[] methods = Example06.class.getDeclaredMethods();
        Method method = methods[0];
        assertNotNull(method);
        assertEquals("divide", method.getName());
        MethodPattern methodPattern = new MethodPattern("rest", "/divide.do", "POST", MediaType.APPLICATION_XML, Example06.class, null, method);
        assertFalse(methodPattern.validatePattern("rest/divide.do?zip=1234", "GET"));
        assertFalse(methodPattern.validatePattern("rest/divide.do", "GET"));

        assertFalse(methodPattern.validatePattern("rest/divide.do?zip=1234", "POST"));
        assertTrue(methodPattern.validatePattern("rest/divide.do", "POST"));
    }

    class Example07 {
        @RolesAllowed(value = "admin")
        @GET
        @Path("/search/{a}/{b}")
        public String search(@PathParam("a")String a, @PathParam("b")String b) {
            return String.format("Search '%s' & '%s'", a, b);
        }

        @Roles(value = Roles.READER)
        @GET
        @Path("/find/{a}/{b}")
        public String find(@PathParam("a")String a, @PathParam("b")String b) {
            return String.format("Find '%s' & '%s'", a, b);
        }
    }

    @Test
    public void testExample07Roles() throws Exception {
        Method[] methods = Example07.class.getDeclaredMethods();
        assertEquals(2, methods.length);
        Method method = Example07.class.getMethod("search", String.class, String.class);
        MethodPattern methodPattern = new MethodPattern("rest", "/search/{a}/{b}", "GET", MediaType.APPLICATION_JSON, Example07.class, null, method);
        assertTrue(methodPattern.validatePattern("rest/search/pc/cpu", "GET"));
        assertArrayEquals(new String[] {"admin"}, methodPattern.getAllowedRoles());

        method = Example07.class.getMethod("find", String.class, String.class);
        methodPattern = new MethodPattern("rest", "/find/{a}/{b}", "GET", MediaType.APPLICATION_JSON, Example07.class, null, method);
        assertTrue(methodPattern.validatePattern("rest/find/pc/memory", "GET"));
        assertArrayEquals(new String[] {"reader"}, methodPattern.getAllowedRoles());
    }

    @Test
    public void testGeneratorPattern() throws Exception {
        Method[] methods = Example02.class.getDeclaredMethods();
        Method method = Example02.class.getMethod("login", String.class);
        assertNotNull(method);
        assertEquals("login", method.getName());
        String pattern = PathPattern.generatorPattern(method);
        assertEquals("/login/{login}", pattern);

        methods = Example03.class.getDeclaredMethods();
        method = methods[0];
        assertNotNull(method);
        assertEquals("customer", method.getName());
        pattern = PathPattern.generatorPattern(method);
        assertEquals("/customer/{customerId : \\d+}", pattern);

        methods = Example04.class.getDeclaredMethods();
        method = methods[0];
        assertNotNull(method);
        assertEquals("zip", method.getName());
        pattern = PathPattern.generatorPattern(method);
        assertEquals("/zip?{zip=\\d+}?{city}?{dumy}", pattern);
    }


    @Test
    public void testQueryParam() throws Exception {
        Method method = SampleApiService.class.getMethod("query", int.class, int.class, String.class);
        assertNotNull(method);
        assertEquals("query", method.getName());
        MethodPattern methodPattern = new MethodPattern("calc", "/query?{from}&{to}&{order}", "GET", MediaType.TEXT_PLAIN, SampleApiService.class, null, method);

        Map<String, Object> args = new HashMap<String, Object>();
        int hit = methodPattern.hit("calc/query?from=10&to=999&order=%5BItem1%2C+Item2%2C+Item3%5D", "GET", args);
        assertEquals(0, hit);
        assertEquals(3, args.size());
        assertEquals("10", args.get("from"));
        assertEquals("999", args.get("to"));
        assertEquals("[Item1, Item2, Item3]", args.get("order"));
    }

    @Test
    public void testQueryParamWithoutPattern() throws Exception {
        Method method = SampleApiService.class.getMethod("query", int.class, int.class, String.class);
        assertNotNull(method);
        assertEquals("query", method.getName());
        MethodPattern methodPattern = new MethodPattern("calc", "/query", "GET", MediaType.TEXT_PLAIN, SampleApiService.class, null, method);

        Map<String, Object> args = new HashMap<String, Object>();
        int hit = methodPattern.hit("calc/query?from=10&to=999&order=%5BItem1%2C+Item2%2C+Item3%5D", "GET", args);
        assertEquals(0, hit);
        assertEquals(3, args.size());
        assertEquals("10", args.get("from"));
        assertEquals("999", args.get("to"));
        assertEquals("[Item1, Item2, Item3]", args.get("order"));
    }


    class Example08 {
        @POST
        @Path("msg/{channel}/reg?{token=\\w+}")
        @Produces(MediaType.APPLICATION_JSON)
        public void register(@PathParam("channel")String channel, @QueryParam("token")String token, @BeanParam String message) {
        }

    }

    @Test
    public void testExample08PathQueryParam() throws Exception {
        Method[] methods = Example08.class.getDeclaredMethods();
        assertEquals(1, methods.length);
        Method method = Example08.class.getMethod("register", String.class, String.class, String.class);
        MethodPattern methodPattern = new MethodPattern("msg", "{channel}/reg?{token=\\w+}", "POST", MediaType.APPLICATION_JSON, Example08.class, null, method);
        assertTrue(methodPattern.validatePattern("msg/ch1/reg?token=1a2b3c4d", "POST"));
    }

}
