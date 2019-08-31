package net.tiny.ws.rs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.JsonParser;
import net.tiny.ws.BaseWebService;
import net.tiny.ws.HttpHandlerHelper;
import net.tiny.ws.RequestHelper;
import net.tiny.ws.ResponseHeaderHelper;

public class RestfulHttpHandler extends BaseWebService {

    private Application application;
    private RestServiceHandler.Listener listener;
    private RestServiceFactory factory;

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
        // Get a instance of MethodPattern
        RestServiceHandler handler = getRestServiceHandler(request, args);
        if (null == handler) {
            // Not found service
            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
            LOGGER.fine(String.format("[REST] - '%s' 404 Not found", request.getURI()));
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

        Object[] params = getRestServiceFactory().convertArguments(he, args, handler.getMethod(), contents);
        try {
            Object result = handler.invoke(params);
            final String response = JsonParser.marshal(result);
            final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
            final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
            header.setContentType(MIME_TYPE.JSON);
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponse.length);
            he.getResponseBody().write(rawResponse);
        } catch (ApplicationException error) {
            he.sendResponseHeaders(error.getStatus(), -1);
        }
    }

    private RestServiceHandler getRestServiceHandler(RequestHelper request, Map<String, Object> args) throws IOException {
        return getRestServiceFactory().getRestServiceHandler(request.getURI(), request.getMethod(), args);
    }
}
