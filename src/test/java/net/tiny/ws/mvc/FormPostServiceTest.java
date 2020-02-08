package net.tiny.ws.mvc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.ws.AccessLogger;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.WebServiceHandler;
import net.tiny.ws.client.SimpleClient;
import net.tiny.ws.mvc.HtmlRenderer;
import net.tiny.ws.rs.RestApplication;
import net.tiny.ws.rs.RestServiceLocator;
import net.tiny.ws.rs.RestfulHttpHandler;

public class FormPostServiceTest {


    static int port;
    static EmbeddedServer server;

    @BeforeAll
    public static void setUp() throws Exception {

        LogManager.getLogManager().readConfiguration(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));

        RestApplication application = new RestApplication();
        application.setPattern("net.tiny.ws.mvc.*, !java.*, !javax.*, !com.sun.*, !org.junit.*,");
        RestServiceLocator context = new RestServiceLocator();
        context.bind("application", application, true);

        HtmlRenderer renderer = new HtmlRenderer();
        AccessLogger logger = new AccessLogger();

        RestfulHttpHandler rest = new RestfulHttpHandler();
        rest.path("/test/form");
        rest.setContext(context);
        rest.setRenderer(renderer);
        rest.setupRestServiceFactory();

        WebServiceHandler restful = rest.filters(Arrays.asList(logger));


        server = new EmbeddedServer.Builder()
                .random()
                .handlers(Arrays.asList(restful))
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
    public void testPostWithFormParam() throws Exception {
        SimpleClient client = new SimpleClient.Builder()
                .userAgent(SimpleClient.BROWSER_AGENT)
                .keepAlive(true)
                .build();
        String request = "arg1=ABC123&arg2=12345";
        String response = client.doPost("http://localhost:" + port +"/test/form/post", request);
        assertNotNull(response);
        System.out.println(response);
        client.close();
    }


    @Test
    public void testPostToMap() throws Exception {
        SimpleClient client = new SimpleClient.Builder()
                .userAgent(SimpleClient.BROWSER_AGENT)
                .keepAlive(true)
                .build();
        String request = "arg1=ABC123&arg2=12345";
        String response = client.doPost("http://localhost:" + port +"/test/form/map", request);
        assertNotNull(response);
        System.out.println(response);
        client.close();
    }

    @Path("/test/form")
    public static class TestFormPostService {

        @POST
        @Path("post")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(value = MediaType.TEXT_HTML)
        public ModelAndView post(
                @FormParam("arg1") final String arg1,
                @FormParam("arg2") final String arg2) {

            ModelAndView mv = new ModelAndView("form.html");
            if (null != arg1) {
                mv.setParam("arg1", arg1);
            }
            if (null != arg1) {
                mv.setParam("arg2", arg2);
            }
            Properties prop = new Properties();
            prop.setProperty("title", "HTML Form POST Sample");
            mv.addParams(prop);
            return mv;
        }

        @POST
        @Path("map")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(value = MediaType.TEXT_HTML)
        public ModelAndView map(final Map<Object, Object> params) {
            ModelAndView mv = new ModelAndView("form.html");
            Properties prop = new Properties();
            prop.setProperty("title", "HTML Form POST Sample");
            mv.addParams(prop);
            mv.addParams(params);
            return mv;
        }
    }
}
