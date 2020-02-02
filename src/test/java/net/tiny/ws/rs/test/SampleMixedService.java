package net.tiny.ws.rs.test;

import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import net.tiny.ws.mvc.ModelAndView;

@Path("/")
public class SampleMixedService {

    @GET
    @Path("api/v3/math/{a}/{b}")
    @Produces(MediaType.APPLICATION_JSON)
    public String math(@PathParam("a")float a, @PathParam("b")float b) {
        return String.format(" %1$.3f + %2$.3f = %3$.3f", a, b, (a+b));
    }

    @POST
    @Path("ui/user/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = MediaType.TEXT_HTML)
    public ModelAndView add(final Map<Object, Object> params) {
        ModelAndView mv = new ModelAndView("form.html");
        Properties prop = new Properties();
        prop.setProperty("title", "HTML Form POST Sample");
        mv.addParams(prop);
        mv.addParams(params);
        return mv;
    }
}
