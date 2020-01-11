package net.tiny.ws.mvc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.tiny.ws.mvc.StringEscape;


public class StringEscapeTest {

    @Test
    public void testEscapeHtml() throws Exception {
        String str = StringEscape.escapeHtml("&<>\"' abc123");
        assertEquals("&amp;&lt;&gt;&quot;'&nbsp;abc123", str);
    }

    @Test
    public void testUnescapeHtml() throws Exception {
        String str = "&amp;&lt;&gt;&quot;&#39;&nbsp;abc123";
        String javaString = StringEscape.unescapeHtml(str);
        System.out.println(javaString);
        assertEquals("&<>\"' abc123", javaString);
    }
}
