package net.tiny.ws.mvc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.tiny.ws.mvc.MessageResources;

import java.util.Locale;

public class MessageResourcesTest  {


    @Test
    public void testLoadMessages() throws Exception {
        MessageResources  messages = MessageResources.valueOf("src/test/resources/config/messages.properties");
        assertNotNull(messages);
        assertEquals(3, messages.getBasenames().size());

        messages = MessageResources.valueOf("config/messages.yml");
        assertNotNull(messages);
        assertEquals(3, messages.getBasenames().size());
        Locale locale = new Locale("zh", "CN");
        assertEquals(locale, messages.getLocale());
        String code = "admin.password.memberNotExist";
        String message = messages.getMessage(code, locale);
        assertEquals("该用户不存在", message);
        assertEquals("This member is not exist.", messages.getMessage(code, new Locale("en", "US")));
    }

    @Test
    public void testGetMessage() throws Exception {
        Locale locale = new Locale("zh", "CN");
        MessageResources messages = MessageResources.valueOf("config/messages.yml");

        String code = "validate.rangelength";
        String message = messages.getMessage(code, locale, 200, 500);
        assertEquals("长度必须在200-500之间", message);
        assertEquals("The length range must be between 200-500", messages.getMessage(code, new Locale("en", "US"), 200, 500));
    }

}
