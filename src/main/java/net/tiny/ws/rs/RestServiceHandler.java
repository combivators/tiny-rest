package net.tiny.ws.rs;

import java.lang.reflect.Method;

public interface RestServiceHandler {

    interface Listener {
        void called(String target, int id, String method, Object[] args);
        void error(Throwable err, String method, Object[] args);
        void param(String annotation, String type, String json);
        void bound(String wrapper, String path);
    }

    Object invoke(final Object[] args);
    Object getTarget() throws Exception;
    Method getMethod();
    String[] getAllowedRoles();
    boolean acceptableMediaType(String type);

    void setListener(Listener listener);

}
