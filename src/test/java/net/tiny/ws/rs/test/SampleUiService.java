package net.tiny.ws.rs.test;

import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import net.tiny.ws.mvc.ModelAndView;

@Path("/ui")
public class SampleUiService {

    @POST
    @Path("page/post")
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
    @Path("form/map")
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
