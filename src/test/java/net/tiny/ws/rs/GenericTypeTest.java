package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import net.tiny.config.Reflections;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.GenericType;

public class GenericTypeTest {

    @Test
    public void testGenericType() throws Exception {

        GenericType<List<String>> genericType = new GenericType<List<String>>() {};
        assertNotNull(genericType);
        assertEquals(List.class, genericType.getRawType());
        Type type = genericType.getType();
        assertTrue((type instanceof ParameterizedType));
        ParameterizedType parameterizedType = (ParameterizedType)type;
        assertEquals(List.class, parameterizedType.getRawType());
        Class<?> actualClaz = (Class<?>)parameterizedType.getRawType();
        TypeVariable<? extends Class<?>>[] typeParameters = actualClaz.getTypeParameters();
        Type[] reified = parameterizedType.getActualTypeArguments();
        for (int i = 0; i < typeParameters.length; i++) {
            System.out.println(typeParameters[i] + " " + reified[i]);
        }

        Type rawType = parameterizedType.getActualTypeArguments()[0];
        assertEquals(String.class, rawType);
    }

    @Test
    public void testGenericListType() throws Exception {
        Method method = TestBean.class.getMethod("nothing");
        assertNotNull(method);
        Class<?> voidType = method.getReturnType();
        assertNotNull(voidType);
        assertEquals(void.class, voidType);
        System.out.println("voidType " + method.getReturnType().getName());
        void.class.cast(null);

        method = TestBean.class.getMethod("getList");
        assertNotNull(method);
        Class<?> listType = method.getReturnType();
        assertEquals(List.class, listType);
        assertTrue((listType instanceof Type));
        Type genericSuper = listType.getGenericSuperclass();
        assertNull(genericSuper);

        System.out.println("ComponentType " + method.getReturnType().getComponentType());
        System.out.println("GenericString " + method.getReturnType().toGenericString());

        TypeVariable<? extends Class<?>>[] typeParameters = method.getReturnType().getTypeParameters();
        System.out.println("TypeVariable " + typeParameters.length);
        for (int i = 0; i < typeParameters.length; i++) {
            System.out.println(typeParameters[i].getName() + " " + typeParameters[i].getTypeName());
        }
        Type type = (Type)listType;
        System.out.println(type.getTypeName());
        assertFalse(type instanceof ParameterizedType);
        assertFalse(type instanceof GenericArrayType);
        assertFalse(type instanceof WildcardType);
        assertFalse(type instanceof TypeVariable);


        TestBean bean = new TestBean();
        Object ret = method.invoke(bean);
        assertNotNull(ret);
        listType = ret.getClass();
        System.out.println(listType.getName());
        System.out.println("ComponentType " + listType.getComponentType());
        assertTrue(Reflections.isCollectionType(listType));
        Collection c = (Collection)ret;
        assertFalse(c.isEmpty());
        Optional<Object> opt = c.stream().findFirst();
        assertTrue(opt.isPresent());
        //Object e = Array.get(ret, 0);
        assertNotNull(opt.get());
        assertEquals(String.class, opt.get().getClass());

        //assertEquals(ArrayList.class, listType);
        type = (Type)listType;
        System.out.println(type.getTypeName());
        assertFalse(type instanceof ParameterizedType);
        assertFalse(type instanceof GenericArrayType);
        assertFalse(type instanceof WildcardType);
        assertFalse(type instanceof TypeVariable);


        /*
        Type rawType = parameterizedType.getActualTypeArguments()[0];
        assertEquals(String.class, rawType);
        */
    }

    @Test
    public void testGenericCollectionType() throws Exception {
        assertEquals(String.class, GenericTypeHelper.genericCollectionType(new GenericType<List<String>>(){}));
        assertEquals(Long.class, GenericTypeHelper.genericCollectionType(new GenericType<List<Long>>(){}));
        assertEquals(Double.class, GenericTypeHelper.genericCollectionType(new GenericType<List<Double>>(){}));

        assertEquals(String.class, GenericTypeHelper.genericCollectionType(new GenericType<String>(){}));
        assertEquals(Long.class, GenericTypeHelper.genericCollectionType(new GenericType<Long>(){}));
    }

    static class TestBean {
        public List<String> getList() {
            return Arrays.asList("abc", "123");
        }

        public void nothing() {}
    }

}
