package net.tiny.ws.mvc;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import net.tiny.ws.mvc.TemplateParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see https://github.com/MaartenGDev/template-engine
 */
public class TemplateParserTest {

    @Test
    public void testIncludeRegex() throws Exception {
        String template = "<html><title>{{title}}</title><body>{% include \"parts\" %}</body></html>";
        Matcher m = TemplateParser.matcher(TemplateParser.INCLUDE_REGX, template);
        assertTrue(m.find());
        assertEquals("{% include \"parts\" %}", m.group(1));
        assertEquals("parts", m.group(2));

        template = "{{% include \"tools/base64_from\" %}}";
        m = TemplateParser.matcher(TemplateParser.INCLUDE_REGX, template);;
        assertTrue(m.find());
        assertEquals("{% include \"tools/base64_from\" %}", m.group(1));
        assertEquals("tools/base64_from", m.group(2));

        template = "{{% include \"tools/base64-from\" %}}";
        m = TemplateParser.matcher(TemplateParser.INCLUDE_REGX, template);;
        assertFalse(m.find());
    }

    @Test
    public void testExtendsRegex() throws Exception {
        String template = "{% extends \"layout\" %}";
        Matcher m = TemplateParser.matcher(TemplateParser.EXTENDS_REGX, template);
        assertTrue(m.find());
        assertEquals("{% extends \"layout\" %}", m.group(1));
        assertEquals("layout", m.group(2));

        template = "{{% extends \"common/app_layout\" %}}";
        m = TemplateParser.matcher(TemplateParser.EXTENDS_REGX, template);;
        assertTrue(m.find());
        assertEquals("{% extends \"common/app_layout\" %}", m.group(1));
        assertEquals("common/app_layout", m.group(2));

        template = "{{% extends \"common/app-layout\" %}}";
        m = TemplateParser.matcher(TemplateParser.EXTENDS_REGX, template);;
        assertFalse(m.find());
    }

    @Test
    public void testBlockRegex() throws Exception {

        assertTrue(Pattern.compile("(.*?)").matcher("\r\n<p>\r\nWelcome\r\n</p>\r\n").find());
        assertTrue(Pattern.compile("(.*)").matcher("\r\n<p>\r\nWelcome\r\n</p>\r\n").find());
        assertTrue(Pattern.compile(".*").matcher("\r\n<p>\r\n{{welcome}}\r\n</p>\r\n").find());
        assertTrue(Pattern.compile("([\\s\\S]*)").matcher("\r\n<p>\r\n{{welcome}}\r\n</p>\r\n").find());

        String template = "<section>{% block \"welcome\" %}<p>Welcome</p>{% endblock %}</section>";
        Matcher m = TemplateParser.matcher(TemplateParser.BLOCK_REGX, template);
        assertTrue(m.find());
        assertEquals("{% block \"welcome\" %}<p>Welcome</p>{% endblock %}", m.group(1));
        assertEquals("welcome", m.group(2));
        assertEquals("<p>Welcome</p>", m.group(3));

        template = "{% block \"welcome\" %}\r\n<p>{{welcome}}</p>\r\n{% endblock %}";
        m = TemplateParser.matcher(TemplateParser.BLOCK_REGX, template);;
        assertTrue(m.find());
        assertEquals("{% block \"welcome\" %}\r\n<p>{{welcome}}</p>\r\n{% endblock %}", m.group(1));
        assertEquals("welcome", m.group(2));
        assertEquals("\r\n<p>{{welcome}}</p>\r\n", m.group(3));
    }

    @Test
    public void testBlocksRegex() throws Exception {
        String template = "{% block \"one\" %}\r\n<h1>{{one}}</h1>\r\n{% endblock %}\r\n" +
                          "{% block \"two\" %}\r\n<h2>{{two}}</h2>\r\n{% endblock %}\r\n" +
                          "{% block \"three\" %}\r\n<p>{{three}}</p>\r\n{% endblock %}\r\n";
        Matcher m = TemplateParser.matcher(TemplateParser.BLOCK_REGX, template);
        int i=0;
        while (m.find()){

            String blockWrapper = m.group(1);
            String blockKey = m.group(2);
            String blockContent = m.group(3);

            switch (i) {
            case 0:
                assertTrue(blockWrapper.startsWith("{% block \"one\" %}") && blockWrapper.endsWith("{% endblock %}"));
                assertEquals("one", blockKey);
                assertEquals("\r\n<h1>{{one}}</h1>\r\n", blockContent);
                break;
            case 1:
                assertTrue(blockWrapper.startsWith("{% block \"two\" %}") && blockWrapper.endsWith("{% endblock %}"));
                assertEquals("two", blockKey);
                assertEquals("\r\n<h2>{{two}}</h2>\r\n", blockContent);
                break;
            case 2:
                assertTrue(blockWrapper.startsWith("{% block \"three\" %}") && blockWrapper.endsWith("{% endblock %}"));
                assertEquals("three", blockKey);
                assertEquals("\r\n<p>{{three}}</p>\r\n", blockContent);
                break;
            default:
                fail("Not found next group.");
                break;
            }
            i++;
        }
    }
    @Test
    public void testReplaceRegex() throws Exception {
        String template = "{{name}}";
        Matcher m = TemplateParser.matcher(TemplateParser.REPLACE_REGX, template);
        assertTrue(m.find());

        template = "{{{name}}}";
        m = TemplateParser.matcher(TemplateParser.REPLACE_REGX, template);
        assertTrue(m.find());

        template = "{{user.address.post}}";
        m = TemplateParser.matcher(TemplateParser.REPLACE_REGX, template);
        assertTrue(m.find());

        template = "{{user1.name}}";
        m = TemplateParser.matcher(TemplateParser.REPLACE_REGX, template);
        assertTrue(m.find());

        template = "{{user1$name}}";
        m = TemplateParser.matcher(TemplateParser.REPLACE_REGX, template);
        assertFalse(m.find());

        template = "{{user1_name}}";
        m = TemplateParser.matcher(TemplateParser.REPLACE_REGX, template);
        assertFalse(m.find());

        template = "{{user1/name}}";
        m = TemplateParser.matcher(TemplateParser.REPLACE_REGX, template);
        assertFalse(m.find());
    }


    @Test
    public void testTemplateParserFillsPlaceHolderInH1Element() throws Exception {
        TemplateParser templateParser = new TemplateParser();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "helloWorld");

        String output = templateParser.parse("<h1>{{name}}</h1>", parameters);

        assertEquals("<h1>helloWorld</h1>", output);
    }

    @Test
    public void testReplaceHolderInValues() throws Exception {
        TemplateParser templateParser = new TemplateParser();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "hoge");
        parameters.put("age", "27");
        String output = templateParser.parse("<input type=\"text\" name=\"name\" size=\"10\" value=\"{{name}}\">\r\n<input type=\"text\" name=\"age\" size=\"4\" value=\"{{age}}\">",
                    parameters);
        assertEquals("<input type=\"text\" name=\"name\" size=\"10\" value=\"hoge\">\r\n" +
                "<input type=\"text\" name=\"age\" size=\"4\" value=\"27\">", output);
    }

    @Test
    public void testPlaceholderValueGetsEscapedWhenUsingTwoBrackets() throws Exception {
        TemplateParser templateParser = new TemplateParser();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "<h1>helloWorld</h1>");

        String output = templateParser.parse("<div>{{name}}</div>", parameters);

        assertEquals("<div>&lt;h1&gt;helloWorld&lt;/h1&gt;</div>", output);
    }

    @Test
    public void testPlaceholderValueDoesNotGetEscapedWhenUsingThreeBrackets() throws Exception {

        TemplateParser templateParser = new TemplateParser();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "<h1>helloWorld</h1>");

        String output = templateParser.parse("<div>{{{name}}}</div>", parameters);

        assertEquals("<div><h1>helloWorld</h1></div>", output);
    }


    @Test
    public void testIfBlockContentIsOnlyShownWhenTruthy() throws Exception {
        TemplateParser templateParser = new TemplateParser();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("isLoggedIn", true);

        String output = templateParser.parse("<section>{% if isLoggedIn %}<p>Welcome</p>{% endif %}</section>", parameters);

        assertEquals("<section><p>Welcome</p></section>", output);
    }

    @Test
    public void testIfBlockContentIsOnlyShownWhenTruthyWithNegatedIf() throws Exception {
        TemplateParser templateParser = new TemplateParser();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("isLoggedIn", false);

        String output = templateParser.parse("<section>{% if not isLoggedIn %}\r\n<p>Not logged in</p>\r\n{% endif %}</section>", parameters);

        assertEquals("<section>\r\n<p>Not logged in</p>\r\n</section>", output);
    }

    @Test
    public void testOnlyContentOfFirstTruthyIfIsShown() throws Exception {
        TemplateParser templateParser = new TemplateParser();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("isLoggedIn", false);
        parameters.put("isAdmin", true);

        String output = templateParser.parse("<section>{% if isLoggedIn %}<p>Not logged in</p>{% elseif isAdmin %}<p>Hello non authenticated admin</p>{% endif %}</section>", parameters);

        assertEquals("<section><p>Hello non authenticated admin</p></section>", output);
    }

    @Test
    public void testContentOfElseIsShownWhenThereAreNoTruthyIfStatements() throws Exception {
        TemplateParser templateParser = new TemplateParser();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("isLoggedIn", false);
        parameters.put("isAdmin", false);

        String output = templateParser.parse("<section>{% if isLoggedIn %}<p>Not logged in</p>{% elseif isAdmin %}<p>Hello non authenticated admin</p>{% else %}<p>Not logged in and no admin</p>{% endif %}</section>", parameters);

        assertEquals("<section><p>Not logged in and no admin</p></section>", output);
    }

    @Test
    public void testPropertiesOfObjectInListGetUsedForPlaceholdersInForBlock() throws Exception {
        TemplateParser templateParser = new TemplateParser();
        Map<String, Object> parameters = new HashMap<>();
        List<Map<String, Object>> users = new ArrayList<>();

        HashMap<String, Object> firstUser = new HashMap<>();
        firstUser.put("name", "First User");

        HashMap<String, Object> secondUser = new HashMap<>();
        secondUser.put("name", "Second User");
        users.add(firstUser);
        users.add(secondUser);
        parameters.put("users", users);

        String output = templateParser.parse("<ul>{% for user in users %}\r\n<li>{{user.name}}</li>\r\n{% endfor %}</ul>", parameters);

        assertEquals("<ul><li>First User</li><li>Second User</li></ul>", output);
    }

    @Test
    public void testReplaceContentBlocks() throws Exception {
        TemplateParser templateParser = new TemplateParser();

        Map<String, Object> parameters = new HashMap<>();

        String template = "<section>{% block \"welcome\" %}<p>Welcome</p>{% endblock %}</section><div>{{{welcome}}}</div>";
        String output = templateParser.parse(template, parameters);
        assertEquals("<section><p>Welcome</p></section><div><p>Welcome</p></div>", output);

        parameters = new HashMap<>();
        parameters.put("welcome", "<h1>Hello, Hoge</h2>");
        output = templateParser.parse(template, parameters);
        assertEquals("<section><p>Welcome</p></section><div><h1>Hello, Hoge</h2></div>", output);

    }

    @Test
    public void testIncludePartials() throws Exception {
        TemplateParser templateParser = new TemplateParser();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", "Hello, Welcome");
        parameters.put("who", "Combivators");
        String template = "<html><title>{{title}}</title><body>{% include \"parts\" %}</body></html>";
        String output = templateParser.parse(template, parameters);
        System.out.print(output);
        assertEquals("<html><title>Hello, Welcome</title><body><div><h1>Hello, Combivators</h1></div></body></html>", output);
    }

    @Test
    public void testIncludeOnly() throws Exception {
        TemplateParser templateParser = new TemplateParser();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", "Hello, Welcome");
        parameters.put("who", "Combivators");
        String template = "{% include \"parts\" %}";
        String output = templateParser.parse(template, parameters);
        System.out.print(output);
        assertEquals("<div><h1>Hello, Combivators</h1></div>", output);
    }


    @Test
    public void testMatcherInclude() throws Exception {
        String template = "{{% include \"tools/base64_from\" %}}";
        Matcher m = Pattern.compile("(\\{% include \"?([\\w/]+)\"? %})")
                .matcher(template);
        assertTrue(m.find());
    }

    @Test
    public void testIncludeParentTemplate() throws Exception {
        TemplateParser templateParser = new TemplateParser();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", "Hello, Welcome");
        parameters.put("who", "Combivators");
        parameters.put("year", "2019");
        String template = new String(Files.readAllBytes(Paths.get("src/test/resources/template/child.html")));
        String output = templateParser.parse(template, parameters);
        System.out.print(output);
        assertTrue(output.contains("<title>Hello, Welcome</title>"));
        assertTrue(output.contains("<h1>Hello, Combivators</h1>"));
        assertTrue(output.contains("<div>&copy; 2019 Combivators</div>"));
    }

}
