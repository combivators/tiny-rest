package net.tiny.ws.rs;

import java.lang.reflect.Method;

public interface RestServiceHandler {

    interface Listener {
        void called(String target, int id, String method);
        void error(Throwable err, String method, Object[] args);
    }

    Object invoke(final Object[] args);
    Object getTarget() throws Exception;
    Method getMethod();
//	Mode getMode();
//	Class<?> getResponseType();
//	String[] getRequestTypes();
//	String[] getMediaTypes();
    String[] getAllowedRoles();
    boolean acceptableMediaType(String type);

    void setListener(Listener listener);

}
