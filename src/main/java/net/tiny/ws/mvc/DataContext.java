package net.tiny.ws.mvc;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.tiny.config.JsonParser;

/**
 * Data object which contains both the data loaded from the given JSON text file,
 * as well as variables created by the template as it is processed.
 *
 */
public class DataContext implements Supplier<Map<String, Object>> {
    private final Map<String, Object> context;

    public DataContext() {
        this(new LinkedHashMap<>());
    }

    protected DataContext(Map<String, Object> context) {
        this.context = context;
    }

    public void clear() {
        context.clear();
    }

    public DataContext add(String key) {
        context.put(key, null);
        return this;
    }

    public DataContext add(String key, Object value) {
        context.putIfAbsent(key, value);
        return this;
    }

    public DataContext put(String key, Object value) {
        context.put(key, value);
        return this;
    }

    public DataContext remove(String key) {
        if (context.containsKey(key)) {
            context.remove(key);
        }
        return this;
    }

    public Map<String, Object> get() {
        return context;
    }

    public Object get(String token) {
        return getValue(token, context);
    }

    @SuppressWarnings("unchecked")
    private Object getValue(String key, Map<String,Object> map) {
        int nextDotIndex = key.indexOf(".");
        String property = null;
        String path = null;
        if (nextDotIndex > -1) {
            property = key.substring(0, nextDotIndex);
            path = key.substring(nextDotIndex+1); // cut out the dot
        } else {
            property = key;
        }
        if (map.containsKey(property)) {
            Object value = map.get(property);
            if (value.getClass() == java.util.LinkedHashMap.class && path != null) {

                Map<String, Object> valueMap = (Map<String, Object>) value;
                return this.getValue(path, valueMap);
            } else {
                return value;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapper(Object target) {
        Map<String, Object> json = null;
        if (target == null)
            return null;
        if (target instanceof Map) { // Return map
            json = (Map<String, Object>)target;
        } else if (target instanceof String) { // Return json map
            json = JsonParser.unmarshal((String)target, Map.class);
        } else if (target instanceof File) { // Return json file map
            File jsonFile = (File)target;
            if (jsonFile.exists()) {
                FileReader reader = null;
                try {
                    reader = new FileReader(jsonFile);
                    json = JsonParser.unmarshal(reader, Map.class);
                    reader.close();
                } catch (IOException e) {
                    throw new IllegalArgumentException(String.format("Can not read input json file '%s'", jsonFile.getAbsolutePath()));
                }
            }
        } else {
            json = JsonParser.unmarshal(JsonParser.marshal(target), Map.class);
        }
        return json;
    }


    public static DataContext valueOf(Object target) {
        return new DataContext(mapper(target));
    }
}
