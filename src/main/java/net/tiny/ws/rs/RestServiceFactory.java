package net.tiny.ws.rs;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.Converter;
import net.tiny.config.JsonParser;
import net.tiny.service.ClassFinder;
import net.tiny.service.ClassHelper;
import net.tiny.service.ServiceContext;
import net.tiny.ws.BaseWebService;

public class RestServiceFactory {

    private static Logger LOGGER = Logger.getLogger(RestServiceFactory.class.getName());
    public static final String REST_PATH = "/rest";
    private static final String REGEX_COOKIE_NAME_VALUE = "^(\\w+)=(.*)$";
    private static final Pattern COOKIE_PATTERN = Pattern.compile(REGEX_COOKIE_NAME_VALUE);
    private final String path;
    private ServiceContext serviceContext;
    private Vector<RestServiceWrapper> servicePatterns = new Vector<RestServiceWrapper>();
    private boolean initing = false;
    private boolean changed = true;
    private Converter converter = new Converter();
    private RestServiceHandler.Listener listener;

    public RestServiceFactory(String path, ServiceContext sc, RestServiceHandler.Listener listener) {
        this.path = path;
        this.listener = listener;
        setServiceContext(sc);
    }

    public ServiceContext getServiceContext() {
        return this.serviceContext;
    }

    public void setServiceContext(ServiceContext sc) {
        this.serviceContext = sc;
        this.changed = true;
        try {
            setup();
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    String.format("Restfull application setup failed - %s", e.getMessage()), e);
        }
    }

    /**
     * 初始化RestService配置
     */
    public synchronized void setup()  {
        if(!changed)
            return;
        initing = true;
        servicePatterns.clear();
        try {
            final RestApplication application = serviceContext.lookup(RestApplication.class);
            // Find and load pattern classes about 2s.
            final ClassFinder classFinder = application.getClassFinder();

            final Set<Class<?>> serviceClasses = application.getClasses();
            final Map<String, Object> cached = application.getProperties();
            for(Class<?> serviceClass : serviceClasses) {
                RestServiceWrapper wrapper = (RestServiceWrapper)cached.get(serviceClass.getName());
                if (null == wrapper) {
                    wrapper = new RestServiceWrapper(serviceClass.newInstance(), listener);
                    cached.put(serviceClass.getName(), wrapper);
                }
                if (wrapper.matches(path)) {
                    servicePatterns.add(wrapper);
                    if (listener != null) {
                        listener.bound( wrapper.toString(), path);
                    }
                 }
            }
            if(servicePatterns.isEmpty()) {
                throw new RuntimeException("One REST service also could not be found.");
            }
            // 对配置项进行排序，方便后面的查找
            Collections.sort(servicePatterns);
            //检查是否有重复的url
            //checkDuplicateUrl();

            // Move RestServiceLocator#injectResource to here
            // Find all rest services and inject resource.
            int count = 0;
            final Map<Class<?>, Supplier<?>> suppliers = new HashMap<>();
            for (RestServiceWrapper wrapper : servicePatterns) {
                // Find a field with @Resource
                List<Field> withResouceAnnotatedFields = ClassHelper.findAnnotatedFields(wrapper.getServiceClass(), Resource.class);
                for(Field field : withResouceAnnotatedFields) {
                    if (injectResource(classFinder, suppliers, wrapper.getService(), field)) {
                        count++;
                    }
                }
            }
            LOGGER.info(String.format("[REST] '%s' Injected %s fields with @Resouce", path, count));
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            initing = false;
            changed = false;
        }
    }

    @SuppressWarnings("unchecked")
    boolean injectResource(ClassFinder classFinder, Map<Class<?>, Supplier<?>> suppliers, Object bean, Field field) {
        try {
            final Class<?> resourceType = Class.forName(field.getGenericType().getTypeName());
            final Class<? super Supplier<?>> supplierType =
                    (Class<? super Supplier<?>>)classFinder.findSupplier(resourceType);
            if (supplierType != null && !suppliers.containsKey(resourceType)) {
                suppliers.put(resourceType, (Supplier<?>)supplierType.newInstance());
            }

            field.setAccessible(true);
            Object res = null;
            if (supplierType != null) {
                Supplier<?> supplier = suppliers.get(resourceType);
                res = supplier.get();
            } else {
                res = serviceContext.lookup(resourceType);
            }
            if(null != res) {
                field.set(bean, res);
                // Injection
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(String.format("[REST] Injection @Resouce of '%s.%s'",
                        field.getDeclaringClass().getSimpleName(),
                        field.getName()));
                }
            } else {
                LOGGER.warning(String.format("[REST] Can not inject @Resouce of '%s.%s'",
                        field.getDeclaringClass().getSimpleName(),
                        field.getName()));
            }

            return true;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOGGER.log(Level.WARNING, String.format("[REST] Not found '%s' Supplier.",
                    field.getGenericType().getTypeName()), e);
            return false;
        }
    }

    /**
     * Servlet通过访问要求取得相应的RestService句柄，并把解析的参数放入Map里
     *
     * @param request
     * @param args
     * @return 句柄
     */
    public RestServiceHandler getRestServiceHandler(final String realUrl, final String httpMthod, final Map<String, Object> args) throws IOException {
        Hitting<?> hit = hit(realUrl, httpMthod, args);
        if(null != hit) {
            return hit.getTarget(RestServiceHandler.class);
        }
        LOGGER.warning(String.format("[REST] - Unmatch '%s' on context '%s'.", realUrl, path));
        return null;
    }

    public Object[] convertArguments(final HttpExchange he, final Map<String, Object> args, final Method method, byte[] requestContents) throws UnsupportedEncodingException {
        Class<?>[] paramTypes = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        Object[] argements = new Object[paramTypes.length];
        for(int i=0; i<argements.length; i++) {
            String key = getParameterKey(annotations[i]);
            if(null != key) {
                argements[i] = convertParameter(args, key, annotations[i], paramTypes[i]);
            } else {
                argements[i] = convertParameter(he, annotations[i], paramTypes[i], requestContents);
            }
        }
        return argements;
    }

    /**
     * 注解
     * @param annotations
     * @return
     */
    private String getParameterKey(Annotation[] annotations) {
        for(Annotation annotation : annotations) {
            if(annotation instanceof PathParam) {
                return ((PathParam)annotation).value();
            } else if(annotation instanceof QueryParam) {
                return ((QueryParam)annotation).value();
            } else if(annotation instanceof MatrixParam) {
                return ((MatrixParam)annotation).value();
            } else if(annotation instanceof FormParam) {
                return ((FormParam)annotation).value();
            }
        }
        return null;
    }

    private Object convertParameter(final Map<String, Object> args, String key, Annotation[] annotations, Class<?> paramType) {
        Object value  = args.get(key);
        if(value != null && value.getClass().isArray() && !paramType.isArray()) {
            value = Array.get(value, 0);
        }
        if(value != null && !paramType.isInstance(value)) {
            value = converter.convert(value.toString(), paramType);
        }
        if(value == null) {
            // Set default value  see @DefaultValue
            for(Annotation annotation : annotations) {
                if(annotation instanceof DefaultValue) {
                    value = converter.convert(((DefaultValue)annotation).value(), paramType);
                }
            }
        }
        return value;
    }

    private Object convertParameter(final HttpExchange he, Annotation[] annotations, Class<?> paramType, byte[] contents) {
        String key = null;
        for(Annotation annotation : annotations) {
            if(annotation instanceof HeaderParam) {
                key = ((HeaderParam)annotation).value();
                String value = he.getRequestHeaders().getFirst(key);
                if(value != null && !paramType.isInstance(value)) {
                    return converter.convert(value.toString(), paramType);
                }
                if (listener != null) {
                    listener.param("@HeaderParam", key, value);
                }
                return value;
            } else if(annotation instanceof CookieParam) {
                key = ((CookieParam)annotation).value();
                String value = getCookie(he, key, true);
                if (listener != null) {
                    listener.param("@CookieParam", key, value);
                }
                return value;
            } else if(annotation instanceof BeanParam) {
                if (null == contents || contents.length == 0) {
                    if (listener != null) {
                        listener.param("@BeanParam", paramType.getSimpleName(), "null");
                    }
                    return null;
                }
                final String json = new String(contents);
                if (listener != null) {
                    listener.param("@BeanParam", paramType.getSimpleName(), json);
                }
                return JsonParser.unmarshal(json, paramType);
            } else if (annotation instanceof Context) {
                //LOGGER.warning("[REST] - Not support @Context parameter type.");
                final Object address = BaseWebService.getClientAddress(he, paramType);
                if (listener != null) {
                    listener.param("@Context", paramType.getSimpleName(), address.toString());
                }
                return address;
                /*
                if(paramType.isAssignableFrom(UriInfo.class)) {
                    //return new UriInfo();
                } else if(paramType.isAssignableFrom(HttpHeaders.class)) {
                    //return new HttpHeaders();
                }
                */
            }
        }
        return null;
    }

    /**
     * Search and retreive a cookie from a HTTP request context
     * @param key, The cookie name to search for
     * @param pReturnJustValue, return just the cookie value or the name + value i.e. "foo=bar;fie;etc";
     * @return
     */
    public String getCookie(HttpExchange he, String key, boolean justValue) {
        Iterator<Map.Entry<String, List<String>>> it =
                he.getRequestHeaders().entrySet().iterator();
        while( it.hasNext()) {
            Map.Entry<String, List<String>> entry = it.next();
            if(entry.getKey().toLowerCase().contentEquals("cookie")){
                String result = getCookieFromSearchString(key, entry.getValue().get(0));
                if(result != null) {
                    if (justValue) {
                        Matcher m = COOKIE_PATTERN.matcher(result);
                        if ((m.matches()) && (m.groupCount() == 2)) {
                            return m.group(2);
                        } else {
                            return result;
                        }
                    }
                }
                return result;
            }
        }
        return null;
    }

    private String getCookieFromSearchString(String key, String wholeCookie) {
        if (wholeCookie.contains(";")) {
            String data[] = wholeCookie.split(";");
            for (int i = 0; i < data.length; i++) {
                if (data[i].trim().startsWith(key)) {
                    return data[i].trim();
                }
            }
        } else if (wholeCookie.startsWith(key)) {
            return wholeCookie;
        }
        return null;
    }

    /**
     * 通过url取得相应的RestService索引
     *
     * @param realUrl
     * @return 索引
     */
    protected Hitting<?> hit(final String realUrl, final String requestMethod,  final Map<String, Object> args) throws IOException {
        if (initing) {
            throw new IllegalStateException();
        }
        int compareRet;
        for (RestServiceWrapper wrapper : servicePatterns) {
            Hitting<?> hit = wrapper.hit(realUrl, requestMethod, args);
            compareRet = hit.getHit();
            if (0 == compareRet) {
                return hit;
            }
        }
        return null;
    }


    /**
     * 通过url取得相应的RestService索引
     *
     * @param realUrl
     * @return 索引
     */
    protected Hitting<?> fastHit(final String realUrl, final String requestMethod,  final Map<String, Object> args) throws IOException {
        //TODO How to be called
        if (initing) {
            throw new IllegalStateException();
        }
        // 折半查找RestService实体
        int low = 0;
        int high = servicePatterns.size();
        int index = (low + high) / 2;
        Hitting<?> hit = servicePatterns.get(index).hit(realUrl, requestMethod, args);
        int compareRet = hit.getHit();
        while (compareRet != 0 && ((high - low) != 1)) {
            if (compareRet > 0) {
                high = index;
            } else {
                low = index;
            }
            index = (low + high) / 2;
            hit = servicePatterns.get((low + high) / 2).hit(realUrl, requestMethod, args);
            compareRet = hit.getHit();
        }
        if(0 == compareRet) {
            return hit;
        } else {
            return null;
        }
    }

    public String info(boolean detail) {
        StringBuilder msg = new StringBuilder(getClass().getSimpleName());
        msg.append("@" + hashCode());
        msg.append(String.format(" - [%1$d]", servicePatterns.size()));
        if(detail) {
            if(!servicePatterns.isEmpty()) {
                msg.append("\r\n");
                for(RestServiceWrapper rest : servicePatterns) {
                    msg.append(rest.toString());
                    msg.append("\r\n");
                }
            }
            return msg.toString();
        } else {
            return msg.toString();
        }
    }

    /**
     * 检查是否有重复的url定义
     * @deprecated
     */
    private void checkDuplicateUrl() {
        RestServiceWrapper prePattern = null;
        final Iterator<RestServiceWrapper> iterator = servicePatterns.iterator();
        while (iterator.hasNext()) {
            final RestServiceWrapper pattern = iterator.next();
            if (pattern.compareTo(prePattern) <= 0) {
                throw new RuntimeException(String.format("Duplicate url : '%s'", pattern.getPath()));
            }
            prePattern = pattern;
        }
    }

    public RestServiceWrapper[] getWrapperServices() {
        RestServiceWrapper[] array = new RestServiceWrapper[servicePatterns.size()];
        servicePatterns.copyInto(array);
        return array;
    }

    @Override
    public String toString() {
        return info(false);
    }

}
