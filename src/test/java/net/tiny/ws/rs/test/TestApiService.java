package net.tiny.ws.rs.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/v2/test")
public class TestApiService {
    private String id;

    @GET
    @Path("get/{id}")
    @Produces(value = MediaType.APPLICATION_JSON)
    public String getId(@PathParam("id")String id) {
        setId(id);
        return "Id is " + id;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
