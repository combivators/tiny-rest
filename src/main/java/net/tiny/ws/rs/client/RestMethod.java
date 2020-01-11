package net.tiny.ws.rs.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import net.tiny.ws.rs.Roles;

public class RestMethod {

    private static final String URL_FORMAT = "http%s://%s:%d%s%s";

    private final Method method;
    private final String path;
    private final String pattern;
    private final Class<?> serviceClass;
    private final String httpMethod;
    private final String[] mediaTypes;
    private final Class<?> returnType;
    private String[] requestTypes;
    private String[] allowedRoles;

   public RestMethod(final Method api, String pathPattern) {
       this.method = api;
       this.serviceClass = method.getDeclaringClass();
       if(!method.toGenericString().startsWith("public")) {
           // Is not public method
           throw new IllegalArgumentException(String.format("'%s.%s' is not a public method.",
                   serviceClass.getName(), method.getName()));
       }
       Path annPath  = method.getAnnotation(Path.class);
       if (null == annPath && pathPattern == null) {
           throw new  IllegalArgumentException(String.format("'%s' not found  annotation @Path.", method.getName()));
       }

       String methodPath = pathPattern;
       if (null != annPath) {
           methodPath = annPath.value();
       }
       // Format path string
       if (!methodPath.startsWith("/")) {
           methodPath = "/".concat(methodPath);
       }
       int pos = methodPath.indexOf("/", 2);
       this.pattern = methodPath.substring(pos);
       this.path = methodPath.substring(0, pos);

       // HTTP Method
       if (method.getAnnotation(GET.class) != null) {
           this.httpMethod = "GET";
       } else if (method.getAnnotation(POST.class) != null) {
           this.httpMethod = "POST";
       } else if (method.getAnnotation(PUT.class) != null) {
           this.httpMethod = "PUT";
       } else if (method.getAnnotation(DELETE.class) != null) {
           this.httpMethod = "DELETE";
       } else {
           this.httpMethod = "GET";
       }

       Produces  produces  = method.getAnnotation(Produces.class);
       if(null != produces) {
           this.mediaTypes = produces.value();
       } else {
           this.mediaTypes = new String[] {MediaType.APPLICATION_JSON};
       }
       Arrays.sort(this.mediaTypes);

       this.returnType = method.getReturnType();
       Consumes consumes = method.getAnnotation(Consumes.class);
       if(null != consumes) {
           this.requestTypes = consumes.value();
       }

       RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
       if(null != rolesAllowed) {
           this.allowedRoles = rolesAllowed.value();
       }

       Roles roles = method.getAnnotation(Roles.class);
       if(null != roles) {
           this.allowedRoles = roles.value();
       }
   }

   public String getHttpMethod() {
       return this.httpMethod;
   }

   public Method getMethod() {
       return this.method;
   }

   public String getPath() {
       return this.path;
   }

   public String getPattern() {
       return this.pattern;
   }

   public Class<?> getResponseType() {
       return this.returnType;
   }

   public String[] getRequestTypes() {
       return this.requestTypes;
   }

   public String[] getMediaTypes() {
       return this.mediaTypes;
   }

   public String[] getAllowedRoles() {
       return this.allowedRoles;
   }

   public String generateURI(Object[] args, String host, int port, boolean secret) {
       final String query = generateQuery(method, args, pattern);
       return String.format(URL_FORMAT, secret ? "s" : "", host, port, path, query);
   }
   public int indexOfBeanParam() {
       return getParameterKeys(method).size();
   }
   public static String generateQuery(Method method, Object[] args, String query) {
       Map<String, Object> params = getParameterMapper(method, args);
       //Query的绑定参数值
       return bingValue(params, query);
   }

   //URL的绑定参数值
   static String bingValue(Map<String, Object> params, String query) {
       StringBuffer sb = new StringBuffer();
       final String[] querySegs = query.split("/");
       for (String seg : querySegs) {
           if (seg.isEmpty()) continue;
           sb.append("/");
           //含”path?{arg1}&{arg2}“ 或是 ”path?{arg1=[regex1]}&{arg2=[regex2]}“情况下解析
           //URI 'query?arg1={arg1}&arg2={arg2}'的情况下解析
           int pos = seg.indexOf("?");
           if (pos > 0) {
               sb.append(seg.substring(0, pos+1));
               seg = seg.substring(pos+1);
               seg = setQueryParameters(params, "&", seg.split("&"));
           } else {
               seg = setQueryParameters(params, "", seg);
           }
           sb.append(seg);
       }
       return sb.toString();
   }

   static String setQueryParameterValue(final Map<String, Object> params, final String seg) {
       int begin = seg.indexOf("{");
       if (begin == -1) {
           // 不含”{...}“定型文字比较
           //URI 'query?name1=value1&name2=value2&name3=value3'的情况下解析
           return seg;
       }
       int end = seg.indexOf("}");
       String key = "";
       //含”{arg : [regex]}“ 或是  ”{arg = [regex]}“文字比较
       String delim = ":";
       int pos = seg.indexOf(delim);
       if (pos == -1) {
           delim = "=";
           pos = seg.indexOf(delim);
       }
       if (pos > begin+1) {
           key = seg.substring(begin+1, pos).trim();
           //String  regex = querySeg.substring(pos+1, querySeg.length()-1).trim();
       } else {
           key = seg.substring(begin+1, seg.length()-1).trim();
       }

       Object value = params.get(key);
       if (null == value) {
           // TODO Set default value
       }
       StringBuffer sb = new StringBuffer();
       if (begin > 0) {
           sb.append(seg.substring(0,  begin));
       }
       sb.append(value.toString());
       if (end < seg.length()-1) {
           sb.append(seg.substring(end,  seg.length()-1));
       }
       return sb.toString();
   }

   private static String setQueryParameters(final Map<String, Object> params, final String delim, final String... segs) {
       StringBuffer sb  = new StringBuffer();
       for (String seg : segs) {
           if (sb.length() > 0) {
               sb.append(delim);
           }
           sb.append(setQueryParameterValue(params, seg));
       }
       return sb.toString();
   }

   static Map<String, Object> getParameterMapper(Method method, Object[] args) {
       List<String> keys = getParameterKeys(method);
       Map<String, Object> params = new LinkedHashMap<>();
       for (int i=0; i<keys.size(); i++) {
           params.put(keys.get(i), args[i]);
       }
       return params;
   }

   private static String getParameterKey(Annotation[] annotations) {
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

   static Map<String, String> parseQuery(String query) {
       Map<String, String> params = new LinkedHashMap<String, String>();
       String[] pair = query.split("&");
       for(int i=0; i<pair.length; i++) {
           String[] nv = pair[i].split("=");
           params.put(nv[0], nv[1]);
       }
       return params;
   }

   static List<String> getParameterKeys(Method method) {
       final Class<?>[] paramTypes = method.getParameterTypes();
       final Annotation[][] annotations = method.getParameterAnnotations();
       final List<String> names = new ArrayList<String>();
       for(int i=0; i<paramTypes.length; i++) {
           String key = getParameterKey(annotations[i]);
           if(null != key) {
               names.add(key);
           }
       }
       return names;
   }

   static String matchQueryParameterKey(final String querySeg) {
       int begin = querySeg.indexOf("{");
       if (begin == -1) {
           // 不含”{...}“定型文字比较
           //URI 'query?name1=value1&name2=value2&name3=value3'的情况下解析
           return null;
       }
       String name = "";
       //含”{arg : [regex]}“ 或是  ”{arg = [regex]}“文字比较
       int pos = querySeg.indexOf(":");
       if (pos == -1) {
           pos = querySeg.indexOf("=");
       }
       if (pos > begin+1) {
           name = querySeg.substring(begin+1, pos).trim();
           //String  regex = querySeg.substring(pos+1, querySeg.length()-1).trim();
       } else {
           name = querySeg.substring(begin+1, querySeg.length()-1).trim();
       }
       return name;
   }

   private static void setQueryParameterKeys(final List<String> names, final String... segs) {
       for (String seg : segs) {
           String name = matchQueryParameterKey(seg);
           if (null != name && !names.contains(name)) {
               names.add(name);
           }
       }
   }

   static List<String> getQueryParameterKeys(final String query) {
       final List<String> names = new ArrayList<String>();
       final String[] querySegs = query.split("/");
       for (String seg : querySegs) {
           //含”path?{arg1}&{arg2}“ 或是 ”path?{arg1=[regex1]}&{arg2=[regex2]}“情况下解析
           //URI 'query?arg1={arg1}&arg2={arg2}'的情况下解析
           int pos = seg.indexOf("?");
           if (pos > 0) {
               seg = seg.substring(pos+1);
               setQueryParameterKeys(names, seg.split("&"));
           } else {
               setQueryParameterKeys(names , seg);
           }
       }
       return names;
   }

}
