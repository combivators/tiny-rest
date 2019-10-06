package net.tiny.ws.rs;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.logging.Level;

import javax.annotation.Resource;

import net.tiny.service.ClassFinder;
import net.tiny.service.ClassHelper;
import net.tiny.service.ServiceLocator;


public class RestServiceLocator extends ServiceLocator {


    @Override
    public void accept(Callable<Properties> callable) {
        super.accept(callable);
        injectResource();
    }

    private void injectResource() {
        final RestfulHttpHandler handler = super.lookup(RestfulHttpHandler.class);
        if (null == handler) {
            LOGGER.warning("[REST] Not found RestfulHttpHandler instance.");
            return;
        }
        final RestApplication application = super.lookup(RestApplication.class);
        if (null == application) {
            LOGGER.warning("[REST] Not found RestApplication instance.");
            return;
        }
        final ClassFinder classFinder = application.getClassFinder();

        Map<Class<?>, Supplier<?>> suppliers = new HashMap<>();

        // Find all rest services and inject resource.
        final RestServiceWrapper[] services = handler.setupRestServiceFactory();
        int count = 0;
        for (RestServiceWrapper wrapper : services) {

            // Find a field with @Resource
            List<Field> withResouceAnnotatedFields = ClassHelper.findAnnotatedFields(wrapper.getServiceClass(), Resource.class);
            for(Field field : withResouceAnnotatedFields) {
                boolean found = false;
                for(RestServiceWrapper s : services) {
                    if (field.getDeclaringClass().equals(s.getServiceClass())) {
                        found = true;
                    }
                }
                if (!found) continue;

                if (inject(classFinder, suppliers, wrapper.getService(), field)) {
                    count++;
                }
                /*
                try {
                    Class<?> resourceType = Class.forName(field.getGenericType().getTypeName());
                    final Class<? super Supplier<?>> supplierType = (Class<? super Supplier<?>>)classFinder.findSupplier(resourceType);
                    if (supplierType != null && !suppliers.containsKey(resourceType)) {
                        suppliers.put(resourceType, (Supplier<?>)supplierType.newInstance());
                    }

                    // Injection
                    LOGGER.fine(String.format("[REST] Injection @Resouce of '%s.%s'",
                            field.getDeclaringClass().getSimpleName(),
                            field.getName()));
                    field.setAccessible(true);
                    count++;
                    if (supplierType != null) {
                        Supplier<?> supplier = suppliers.get(resourceType);
                        field.set(wrapper.getService(), supplier.get());
                    } else {
                        Object res = super.lookup(resourceType);
                        if(null != res) {
                            field.set(wrapper.getService(), res);
                        }
                    }

                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    LOGGER.log(Level.WARNING, String.format("[REST] Not found '%s' Supplier.",
                            field.getGenericType().getTypeName()), e);
                }
                */
            }

        }
        LOGGER.info(String.format("[REST] Injected %s fields with @Resouce", count));
    }

    @SuppressWarnings("unchecked")
    protected boolean inject(ClassFinder classFinder, Map<Class<?>, Supplier<?>> suppliers, Object bean, Field field) {

        try {
            final Class<?> resourceType = Class.forName(field.getGenericType().getTypeName());
            final Class<? super Supplier<?>> supplierType =
                    (Class<? super Supplier<?>>)classFinder.findSupplier(resourceType);
            if (supplierType != null && !suppliers.containsKey(resourceType)) {
                suppliers.put(resourceType, (Supplier<?>)supplierType.newInstance());
            }

            // Injection
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("[REST] Injection @Resouce of '%s.%s'",
                    field.getDeclaringClass().getSimpleName(),
                    field.getName()));
            }
            field.setAccessible(true);

            if (supplierType != null) {
                Supplier<?> supplier = suppliers.get(resourceType);
                field.set(bean, supplier.get());
            } else {
                Object res = lookup(resourceType);
                if(null != res) {
                    field.set(bean, res);
                }
            }
            return true;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOGGER.log(Level.WARNING, String.format("[REST] Not found '%s' Supplier.",
                    field.getGenericType().getTypeName()), e);
            return false;
        }

    }

    public static class RestServiceMonitor extends ServiceMonitor implements RestServiceHandler.Listener {

        @Override
        public void called(String target, int id, String method) {
            LOGGER.log(Level.INFO, String.format("[REST] Invoked '%d#%s.%s(...)'",	id, target, method));
        }

        @Override
        public void error(Throwable err, String method, Object[] args) {
            Throwable cause = err.getCause();
            if (cause == null) cause = err;
            LOGGER.log(Level.WARNING, String.format("[REST] API method '%s(...)' invoke error : %s ", method, cause.getMessage()), cause);
        }

    }
}
