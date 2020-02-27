## Tiny Rest: 一个基于RESTful协议的微服框架
## 设计目的
 - 使用Tiny Boot包进行服务器配置。
 - 基于Tiny Service的Java类实装REST服务。
 - 支持javax.ws.rs基类的REST实装
 - 提供精简的REST客户端Java类。

##Usage

###1. Simple Run
```java
java net.tiny.boot.Main --verbose
```


###2. Application configuration file with profile
 - Configuration file : application-{profile}.[yml, json, conf, properties]

```yaml
logging:
  handler:
    output: none
  level:
    all: INFO
main:
  - ${launcher.ws}
daemon: true
executor: ${pool}
callback: ${service.context}
pool:
  class:   net.tiny.service.PausableThreadPoolExecutor
  size:    5
  max:     10
  timeout: 3
service:
  context:
    class: net.tiny.service.ServiceLocator
rest:
  application:
    class:   net.tiny.ws.rs.RestApplication
    pattern: net.tiny.message.*, net.tiny.feature.*
    scan:    .*/classes/, .*/feature-.*[.]jar, .*/tiny-.*[.]jar, !.*/tiny-dic.*[.]jar
    verbose: false
#
launcher:
  ws:
    class: net.tiny.ws.Launcher
    builder:
      port: 8080
      backlog: 10
      stopTimeout: 1
      executor: ${pool}
      handlers:
        - ${handler.sys}
        - ${handler.rest}
        - ${handler.api}
        - ${handler.ui}
handler:
  sys:
    class:   net.tiny.ws.ControllableHandler
    path:    /sys
    auth:    ${auth.base}
    filters: ${filter.logger}
  rest:
    class:     net.tiny.ws.rs.RestfulHttpHandler
    path:      /home
    filters:   ${filter.logger}
    renderer: ${renderer.html}
  api:
    class:     net.tiny.ws.rs.RestfulHttpHandler
    path:      /api
    filters:
      - ${filter.auth}
      - ${filter.logger}
  ui:
    class:     net.tiny.ws.rs.RestfulHttpHandler
    path:      /ui
    filters:
      - ${filter.logger}
      - ${filter.jpa}
    renderer : ${renderer.html}
filter:
   logger:
     class: net.tiny.ws.AccessLogger
     out:   stdout
   auth:
     class:  net.tiny.ws.auth.JsonWebTokenFilter
     validator: ${setting.validator}
auth:
  base:
    class:    net.tiny.ws.auth.SimpleAuthenticator
    encode:   false
    username: admin
    password: password
#
renderer:
  html:
    class:  net.tiny.ws.mvc.HtmlRenderer
    parser: ${template.parser}
    cache:  ${content.cache}
template:
  parser:
    class: net.tiny.ws.mvc.TemplateParser
    path: webapp
    cache: ${content.cache}
content:
  cache:
    class: net.tiny.ws.cache.CacheFunction
    size: 10
```


###3. Sample Rest service java
```java
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/v1")
public class SampleApiService {
    @Resource
    private DataSource dataSource;

    private String id;

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

    @GET
    @Path(value = "cookie")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cookie(@CookieParam("cookie1") String cookie1, @CookieParam("cookie2") String cookie2) {
        String cookies = "cookie1: " + cookie1 +  "  cookie2: " + cookie2;
        Map<String, String> map = new HashMap<>();
        map.put("token", "1234567890abcdef");
        return Response.ok()
                .entity(map)
                .cookie("authToken=" + cookie1 + cookie2)
                .cache(86400L)
                .build();
    }
}
```

###4. Sample Rest client java
```java
import net.tiny.ws.rs.client.RestClient;

RestClient client = new RestClient.Builder()
        .build();

String response = client.execute("http://localhost:8080/api/v1/add/12.3/4.56")
            .get(MediaType.APPLICATION_JSON)
            .getEntity();

client.close();
```

##More Detail, See The Samples

---
Email   : wuweibg@gmail.com
