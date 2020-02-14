package net.tiny.ws.rs;

import java.lang.reflect.ParameterizedType;

import javax.ws.rs.core.GenericType;

import net.tiny.config.Reflections;

public final class GenericTypeHelper {

    public static Class<?> genericCollectionType(GenericType<?> type) {
        Class<?> genericType = type.getRawType();
        if (!Reflections.isCollectionType(genericType)) {
            return genericType;
        }
        ParameterizedType parameterizedType = (ParameterizedType)type.getType();
        genericType = (Class<?>)parameterizedType.getActualTypeArguments()[0];
        return genericType;
    }
}
