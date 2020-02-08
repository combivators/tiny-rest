package net.tiny.ws.rs;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import net.tiny.service.ServiceLocator;


public class RestServiceLocator extends ServiceLocator {


    @Override
    public void accept(Callable<Properties> callable) {
        super.accept(callable);
        injectResource();
    }

    private void injectResource() {
        final Collection<RestfulHttpHandler> handlers = super.lookupGroup(RestfulHttpHandler.class);
        if (null == handlers) {
            LOGGER.warning("[REST] Not found RestfulHttpHandler instance.");
            return;
        }
        final RestApplication application = super.lookup(RestApplication.class);
        if (null == application) {
            LOGGER.warning("[REST] Not found RestApplication instance.");
            return;
        }
        // Find all rest services and inject resource.
        int count = 0;
        for (RestfulHttpHandler handler : handlers) {
            final RestServiceWrapper[] services = handler.setupRestServiceFactory();
            count += services.length;
        }
        LOGGER.info(String.format("[REST] Found %d rest service(s).", count));
    }


    public static class RestServiceMonitor extends ServiceMonitor implements RestServiceHandler.Listener {

        @Override
        public void called(String target, int id, String method, Object[] args) {
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (Object o : args) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(String.valueOf(o));
               }
            }
            LOGGER.log(Level.INFO, String.format("[REST] Invoked '%d#%s.%s(%s)'",	id, target, method, sb.toString()));
        }

        @Override
        public void param(String annotation, String type, String value) {
            LOGGER.log(Level.INFO, String.format("[REST] %s '%s' = %s", annotation, type, value));
        }

        @Override
        public void error(Throwable err, String target, String method, Object[] args) {
            Throwable cause = err.getCause();
            if (cause == null) cause = err;
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (Object o : args) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(String.valueOf(o));
               }
            }
            LOGGER.log(Level.WARNING, String.format("[REST] '%s.%s(%s)' invoke error : %s ",
                    target, method, sb.toString(), cause.getMessage()), cause);
        }

        @Override
        public void bound(String wrapper, String path) {
            LOGGER.log(Level.INFO, String.format("[REST] Context:'%s' bound %s", path, wrapper));
        }

    }
}
