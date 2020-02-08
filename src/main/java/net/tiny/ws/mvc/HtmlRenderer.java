package net.tiny.ws.mvc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import net.tiny.ws.Constants.MIME_TYPE;
import net.tiny.ws.cache.CacheFunction;
import net.tiny.ws.rs.ApplicationException;
import net.tiny.ws.HttpHandlerHelper;
import net.tiny.ws.ResponseHeaderHelper;

public class HtmlRenderer implements ViewRenderer {

    private static Logger LOGGER = Logger.getLogger(HtmlRenderer.class.getName());

    private String prefix = "webapp";
    private TemplateParser parser;
    private CacheFunction cache = null;
    private int cacheSize = -1;

    @Override
    public void setCache(CacheFunction cache) {
        this.cache = cache;
    }

    @Override
    public void render(HttpExchange he, ModelAndView modelView, Annotation[] annotations, MediaType mediaType, Headers httpHeaders) {
        String resource = String.format("%s/%s", prefix, modelView.getViewPath());

        try {
            final Map<String, Object> args = new HashMap<String, Object>();
            Map<String, Object> params = modelView.getParams();
            for (Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                args.put(key, params.get(key));
            }

            if(modelView.hasFlashMessages()) {
                FlashComponent flashComponent = new FlashComponent();
                flashComponent.setMessages(modelView.removeFlashMessages());
                args.put(FlashComponent.ATTRIBUTE_NAME, flashComponent);
            }
            if(modelView.hasViolations()) {
                ViolationComponent violationComponent = new ViolationComponent();
                violationComponent.setViolations(modelView.removeViolations());
                violationComponent.setParams(modelView.removeParams());
                args.put(ViolationComponent.ATTRIBUTE_NAME,	violationComponent);
            }


            final byte[] rawResponse = render(resource, args);
            final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
            header.setContentType(MIME_TYPE.HTML);
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponse.length);
            he.getResponseBody().write(rawResponse);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new ApplicationException(e.getMessage(), e, HttpURLConnection.HTTP_NOT_FOUND);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            final String error = String.format("Unable to write html template '%s' response.", resource);
            throw new ApplicationException(error, e, HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }


    byte[] render(String resource, Map<String, Object> args) throws IOException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (null == url) {
            throw new IOException(String.format("Can not found '%s' template resource.", resource));
        }
        String template = new String(getCacheableContents(url));
        return getParser().parse(template, args)
                          .getBytes();
    }

    public TemplateParser getParser() {
        if (parser == null) {
            parser = new TemplateParser();
            parser.setPath(prefix);
            parser.setCache(cache);
        }
        return parser;
    }

    public void setParser(TemplateParser parser) {
        this.parser = parser;
    }

    private byte[] getCacheableContents(URL url) throws IOException {
        if (cache == null) {
            if (cacheSize > 0) {
                // Cache max files
                cache = new CacheFunction(cacheSize);
            } else {
                cache = new CacheFunction();
            }
        }
        return cache.apply(url);
    }

}
