package net.tiny.ws.rs.test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import net.tiny.ws.rs.ApplicationException;
import net.tiny.ws.rs.Model;

import javax.ws.rs.core.UriInfo;

@Path("/api/v1")
public class SampleApiService {

    @GET
    @Path("add/{a}/{b}")
    @Produces(MediaType.APPLICATION_JSON)
    public String add(@PathParam("a")double a, @PathParam("b")double b) {
        return String.format(" %1$.3f + %2$.3f = %3$.3f", a, b, (a+b));
    }

    @GET
    @Path("plus/{a}/{b}")
    @Produces(MediaType.APPLICATION_JSON)
    public String plus(@PathParam("a")double a, @PathParam("b")double b) {
        String response = String.format(" %1$.3f + %2$.3f = %3$.3f", a, b, (a+b));
        if (response.length() > 10) {
            throw new ApplicationException(HttpURLConnection.HTTP_NOT_FOUND);
        }
        return response;
    }

    //TODO @DefaultValue @QueryParam Not implement!
    @GET
    @Path("query?{from=\\d+}&{to=\\d+}&{order}")
    @Produces(MediaType.APPLICATION_JSON)
    public String query(
        @DefaultValue("100") @QueryParam("from") int from,
        @DefaultValue("999") @QueryParam("to") int to,
        @DefaultValue("name") @QueryParam("order") String orderBy) {

        return "query is called, from : " + from + ", to : " + to
                + ", order by " + orderBy;
    }

    @POST
    @Path("ask/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Consumes(MediaType.APPLICATION_JSON)
    public DummyUser ask(@BeanParam DummyEmail mail) throws IOException {
        int pos = mail.email.indexOf("@");
        DummyUser user = new DummyUser();
        user.name = mail.email.substring(0, pos);
        user.domain = mail.email.substring(pos+1);
        return user;
    }


    @POST
    @Path("divide")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String divide(@FormParam("divisor")Double divisor,
            @FormParam("roundingMode")Double roundingMode) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>JSR311 Example</title></head><body>");
        sb.append(String.format(" %1$.3f + %2$.3f = %3$.3f", divisor, roundingMode, (divisor/roundingMode)));
        sb.append("</body></html>");
        return sb.toString();
    }

    @GET
    @Path(value = "cookie")
    @Produces(MediaType.APPLICATION_JSON)
    public Model cookie(@CookieParam("cookie1") String cookie1, @CookieParam("cookie2") String cookie2) {
        String cookies = "cookie1: " + cookie1 +  "  cookie2: " + cookie2;
        System.out.println("/api/v1/cookie " + cookies);
        /*
        NewCookie c1 = new NewCookie("name1", "The cookie value$1");
        Cookie cookie = new Cookie("name2", "The cookie value%2");
        NewCookie c2 = new NewCookie(cookie);
        ResponseBuilder builder = Response.ok(response);
        return builder.cookie(c1, c2).build();
        */
        Map<String, String> map = new HashMap<>();
        map.put("token", "1234567890abcdef");
        return new Model(map)
                .cookie("authToken=" + cookie1 + cookie2)
                .cache(86400L);

    }

    ///////////////////////////////
    //Not implements Rest Object

    @GET
    @Path("info")
    public Response info(@Context UriInfo info) {
        String from = info.getQueryParameters().getFirst("from");
        String to = info.getQueryParameters().getFirst("to");
        List<String> orderBy = info.getQueryParameters().get("orderBy");
        return Response.status(200)
           .entity("getUsers is called, from : " + from + ", to : " + to
            + ", orderBy" + orderBy.toString()).build();

    }

    @POST
    @Path("post")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void post(@Context UriInfo uri) throws IOException {
        MultivaluedMap<String, String> formParam  = uri.getQueryParameters();
        formParam.getFirst("");
        //TODO
        //response.sendRedirect("");
    }

    public static class DummyEmail {
        public String email;
    }

    public static class DummyUser {
        public String name;
        public String domain;
    }
}

