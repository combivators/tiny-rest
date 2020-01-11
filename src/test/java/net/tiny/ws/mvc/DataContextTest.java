package net.tiny.ws.mvc;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import net.tiny.ws.mvc.DataContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DataContextTest {

    @Test
    public void testJsonKeyValuePairsResultInAHashMap(){
        Map<String, Object> actual = DataContext.mapper("{\"name\": \"helloWorld\"}");

        Map<String, Object> expected = new HashMap<>();
        expected.put("name", "helloWorld");

        assertEquals(expected, actual);
    }

    @Test
    public void testJsonListGetsConvertedToArrayList(){
        Map<String, Object> actual = DataContext.mapper("{\"users\": [{\"name\": \"first\"},{\"name\": \"second\"}]}");

        Map<String, Object> expected = new HashMap<>();
        List<Map<String, Object>> users = new ArrayList<>();

        HashMap<String, Object> firstUser = new HashMap<>();
        firstUser.put("name", "first");

        HashMap<String, Object> secondUser = new HashMap<>();
        secondUser.put("name", "second");

        users.add(firstUser);
        users.add(secondUser);

        expected.put("users", users);

        assertEquals(expected, actual);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testJsonObjectGetsConvertedToHashMap(){
        Map<String, Object> actual = DataContext.mapper("{\"user\": {\"name\": \"first\", \"level\": 6}}");

        Map<String, Object> expected = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        user.put("name", "first");
        user.put("level", 6);

        expected.put("user", user);


        assertEquals("first", ((Map<String, Object>)actual.get("user")).get("name"));
        assertEquals(6.0d, ((Map<String, Object>)actual.get("user")).get("level"));
    }

    @Test
    public void testAddPut() {
        DataContext context = new DataContext()
                .add("who")
                .add("who", "Java")
                .put("time", "17-Mar-2010")
                .add("time", "18-Mar-2010")
                .add("name", "Hoge")
                .remove("name");
        assertEquals("Java", context.get().get("who"));
        assertEquals("17-Mar-2010", context.get().get("time"));
        assertNull(context.get().get("name"));
    }


    @Test
    public void testSingleName() {
        Map<String, Object> map = new HashMap<>();
        map.put("who", "Java");
        map.put("time", "17-Mar-2010");
        map.put("name", "Hoge");
        DataContext context = new DataContext(map);
        assertEquals("Java", context.get().get("who"));
        assertEquals("17-Mar-2010", context.get().get("time"));
        assertEquals("Hoge", context.get().get("name"));
    }

    @Test
    public void testMapper() {
        Map<String, Object> map = DataContext.mapper(new Who());
        assertNotNull(map);
        assertEquals(4, map.size());

        DataContext context = new DataContext()
                .add("who", map);
        assertEquals("Hoge", context.get("who.name"));
        assertEquals("17-Mar-2010", context.get("who.time"));
        assertEquals("Tokyo", context.get("who.address.city"));
        assertEquals("112-0001", context.get("who.address.post"));

    }

    static class Address {
        String city = "Tokyo";
        String post = "112-0001";
    }

    static class Who {
        String name = "Hoge";
        int age = 30;
        String time = "17-Mar-2010";
        Address address = new Address();
    }
}
