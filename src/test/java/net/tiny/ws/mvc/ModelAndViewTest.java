package net.tiny.ws.mvc;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import net.tiny.config.JsonParser;
import net.tiny.ws.mvc.ModelAndView;

public class ModelAndViewTest {

    @Test
    public void testJson() throws Exception {
        ModelAndView mv = new ModelAndView("index.html");
        String json = JsonParser.marshal(mv);
        System.out.println(json);
        assertTrue(json.contains("\"viewPath\":\"index.html\""));
   }

    @Test
    public void testRestProduces() throws Exception {
        Method method = DummyRest.class.getMethod("html");
        assertNotNull(method);
        Produces produces = method.getAnnotation(Produces.class);
        assertEquals(1, produces.value().length);
        assertEquals("text/html", produces.value()[0]);
        assertEquals("text", MediaType.TEXT_HTML_TYPE.getType());
        assertEquals("html", MediaType.TEXT_HTML_TYPE.getSubtype());


        method = DummyRest.class.getMethod("json");
        produces = method.getAnnotation(Produces.class);
        assertEquals("application/json", produces.value()[0]);
        assertEquals("application", MediaType.APPLICATION_JSON_TYPE.getType());
        assertEquals("json", MediaType.APPLICATION_JSON_TYPE.getSubtype());

    }


    static class DummyRest {
        @GET
        @Path("html")
        @Produces(value = MediaType.TEXT_HTML)
        public ModelAndView html() {
            return new ModelAndView("index.html");
        }

        @GET
        @Path("json")
        @Produces(value = MediaType.APPLICATION_JSON)
        public ModelAndView json() {
            return new ModelAndView("index.html");
        }
    }
}
