package net.tiny.ws.rs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.JsonParser;
import net.tiny.ws.BaseWebService;
import net.tiny.ws.HttpHandlerHelper;
import net.tiny.ws.PostParameterPaser;
import net.tiny.ws.RequestHelper;
import net.tiny.ws.ResponseHeaderHelper;
import net.tiny.ws.mvc.ModelAndView;
import net.tiny.ws.mvc.ViewRenderer;

public class RestfulHttpHandler extends BaseWebService {

    private Application application;
    private RestServiceHandler.Listener listener;
    private RestServiceFactory factory;
    private ViewRenderer renderer;

    protected RestServiceFactory getRestServiceFactory() {
        if (null == factory) {
            setupRestServiceFactory();
        }
        return factory;
    }

    public Application getApplication() {
        return this.application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public void setListener(RestServiceHandler.Listener listener) {
        this.listener = listener;
    }

    public void setRenderer(ViewRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Call by RestServiceLocator#accept method
     */
    public RestServiceWrapper[] setupRestServiceFactory() {
        if (null == factory) {
            factory = new RestServiceFactory(listener);
            factory.setApplication(application);
        }
        return factory.getWrapperServices();
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        final RequestHelper request = HttpHandlerHelper.getRequestHelper(he);

        final Map<String, Object> args = new HashMap<>();
        final boolean formRequested = isPageFormRequest(request);
        if (formRequested) {
            // When Form POST, Setting post parameters
            BufferedReader reader = new BufferedReader(new InputStreamReader(he.getRequestBody(), "UTF-8"));
            String query = reader.readLine();
            PostParameterPaser.parseQuery(query, args);
        }
        // Get a instance of MethodPattern
        RestServiceHandler handler = getRestServiceFactory().getRestServiceHandler(request.getURI(), request.getMethod(), args);
        if (null == handler) {
            // Not found service
            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("[REST] - '%s' 404 Not found", request.getURI()));
            }
            return;
        }

        //
//        if (!handler.acceptableMediaType(MIME_TYPE.JSON.name())) {
//            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_ACCEPTABLE, -1);
//            LOGGER.info(String.format("[REST] - '%s' 404 Not found", request.getURI()));
//            return;
//        }

        byte[] contents = null;
        switch(method) {
        case PUT:
        case POST:
            contents = request.getRequestContent();
            break;
        default:
            break;
        }

        final Object[] params = getRestServiceFactory().convertArguments(he, args, handler.getMethod(), contents);
        try {
            if (formRequested && isPageFormMapRequest(params, handler.getMethod())) {
                // When Form POST, Setting Map parameters
                params[0] = args;
            }
            final Object result = handler.invoke(params);
            if (result != null) {
                if (requestedMediaType(handler, MediaType.TEXT_HTML_TYPE) && result instanceof ModelAndView && renderer != null) {
                    // Return text/html response
                    final ModelAndView mv = (ModelAndView)result;
                    mv.setReferer(request.getReferer());
                    renderer.render(he, mv, handler.getMethod().getAnnotations(), MediaType.TEXT_HTML_TYPE, request.getHeaders());
                } else {
                    // Return json response
                    final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
                    final String response = JsonParser.marshal(result);
                    final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
                    header.setContentType(MIME_TYPE.JSON);
                    he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponse.length);
                    he.getResponseBody().write(rawResponse);
                }
            } else {
                he.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
            }
        } catch (ApplicationException err) {

            LOGGER.log(Level.WARNING, String.format("[REST] - %s '%s' %d %s. On call '%s(...)'",
                    request.getMethod(), request.getURI(), err.getStatus(), err.getMessage(), handler.toString()), err);
            he.sendResponseHeaders(err.getStatus(), -1);
        }
    }

    private boolean isPageFormRequest(RequestHelper request) {
        final String contentType = request.getContentType();
        return "POST".equals(request.getMethod())
                && (!contentType.contains("json") || contentType.contains("x-www-form-urlencoded"));
    }

    private boolean isPageFormMapRequest(Object[] params, Method method) {
        return params.length > 0 && params[0] == null
                && method.getParameterCount() == 1
                && Map.class.equals(method.getParameterTypes()[0]);
    }

    private boolean requestedMediaType(RestServiceHandler handler, MediaType mediaType) {
        final Produces produces = handler.getMethod().getAnnotation(Produces.class);
        boolean requested = false;
        for (String type : produces.value()) {
            requested = type.contains(mediaType.getType()) || type.contains(mediaType.getSubtype());
            if (requested)
                break;
        }
        return requested;
    }
}
