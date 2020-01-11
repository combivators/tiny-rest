package net.tiny.ws.mvc;

import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.ws.rs.core.MediaType;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import net.tiny.ws.cache.CacheFunction;

/**
 * interface of all renders 所有渲染器的接口
 *
 *
 */
public interface ViewRenderer {
    /**
     * 返回内容 Produces the response.
     *
     * @param httpExchange
     *            object returned by the JAX-RS method
     * @param modelView
     *            data set of model
     * @param annotations
     *            annotations available on the JAX-RS method
     * @param mediaType
     *            requested media type
     * @param httpHeaders
     *            request HTTP headers
     * @throws IOException
     */
    void render(HttpExchange httpExchange, ModelAndView modelView, Annotation[] annotations, MediaType mediaType, Headers httpHeaders) throws IOException;

    void setCache(CacheFunction cache);
}
