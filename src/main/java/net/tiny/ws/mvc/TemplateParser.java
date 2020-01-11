package net.tiny.ws.mvc;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tiny.ws.cache.CacheFunction;

public class TemplateParser {

    private final static Logger LOGGER  = Logger.getLogger(TemplateParser.class.getName());

    final static String INCLUDE_REGX = "(\\{% include \"?([\\w/]+)\"? %})";
    final static String EXTENDS_REGX = "(\\{% extends \"?([\\w/]+)\"? %})";
    //final static String BLOCK_REGX   = "(\\{% ?block \"?([\\w]+)\"? ?%}(.*?)\\{% ?endblock ?%})";
    final static String BLOCK_REGX   = "(\\{% ?block \"?([\\w]+)\"? ?%}([\\s\\S]*?)\\{% ?endblock ?%})";
    final static String REPLACE_REGX = "((\\{)?\\{\\{ *([a-zA-Z.0-9]+) *}}(})?)";
    //final static String LOOPS_REGX   = "(\\{% for (\\w+) in (\\w+) %}(.*)\\{% endfor %})";
    final static String LOOPS_REGX   = "(\\{% for (\\w+) in (\\w+) %}([\\s\\S]*?)\\{% endfor %})";
    //final static String IFTHEN_REGX  = "(\\{% if (?:not )?(?:\\w+) %}.*\\{% endif %})";
    final static String IFTHEN_REGX  = "(\\{% if (?:not )?(?:\\w+) %}([\\s\\S]*?)\\{% endif %})";
    final static String IFELSE_REGX  = "(\\{% ((?:else)?(?:if)?) (not )?(?:(\\w+) )?%})([^{%]+)";

    private final ContentLocator contentLocator;
    private final CacheFunction cache;

    public TemplateParser(String basePath) {
        this(basePath, new CacheFunction(null));
    }

    public TemplateParser(String basePath, CacheFunction cache) {
        if (basePath.startsWith("/")) {
            this.contentLocator = new FileLocator(basePath);
        } else {
            this.contentLocator = new ResourceLocator(basePath);
        }
        this.cache = cache;
    }

    public String parse(String template, Map<String, Object> parameters) throws IOException {
        template = includeParentTemplate(template, parameters);
        template = includePartials(template, parameters);
        template = replaceContentBlocks(template, parameters);
        template = replaceForLoops(template, parameters);
        template = replaceIfStatements(template, parameters);
        template = replacePlaceholders(template, parameters);
        return template;
    }

    private String includePartials(String template, Map<String, Object> parameters) throws IOException {
        Matcher m = matcher(INCLUDE_REGX, template);

        while (m.find()) {
            String wrapper = m.group(1);
            String partialPath = getTemplateName(m.group(2));

            String partial = contentLocator.get(partialPath);
            template = template.replace(wrapper, partial);
        }

        return template;
    }

    private String replaceContentBlocks(String template, Map<String, Object> parameters) {
        Map<String, ContentBlock> contentBlocks = getContentBlocks(template);

        for(String key : contentBlocks.keySet()){
            ContentBlock block = contentBlocks.get(key);
            template = template.replace(block.wrapper, block.content);
            parameters.putIfAbsent(key, block.content);
        }

        return template;
    }

    private String includeParentTemplate(String template, Map<String, Object> parameters) throws IOException {
        Matcher m = matcher(EXTENDS_REGX, template);
        while (m.find()) {
            String extendsBlock = m.group(1);
            String templatePath = getTemplateName(m.group(2));

            String parentTemplate = contentLocator.get(templatePath);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("Use '%s' template layout." , templatePath));
            }
            Map<String, ContentBlock> parentBlocks = getContentBlocks(parentTemplate);
            Map<String, ContentBlock> childrenBlocks = getContentBlocks(template);

            String replaced = parentTemplate;
            for(String key : parentBlocks.keySet()){
                boolean childOverridesBlock = childrenBlocks.containsKey(key);
                String content = childOverridesBlock ? childrenBlocks.get(key).content : parentBlocks.get(key).content;
                content = parse(content, parameters);
                replaced = replaced.replace(parentBlocks.get(key).wrapper, content);
            }
            template = replaced;
            template = template.replace(extendsBlock, "");
        }
        return template;
    }

    private Map<String, ContentBlock> getContentBlocks(String template){
        Matcher m = matcher(BLOCK_REGX, template);
        Map<String, ContentBlock> blocks = new HashMap<>();

        while (m.find()){
            String blockWrapper = m.group(1);
            String blockKey = m.group(2);
            String blockContent = m.group(3);

            ContentBlock contentBlock = new ContentBlock();
            contentBlock.wrapper = blockWrapper;
            contentBlock.content = blockContent;
            blocks.put(blockKey, contentBlock);
        }

        return blocks;
    }


    private String replacePlaceholders(String template, Map<String, Object> parameters) {
        Matcher m = matcher(REPLACE_REGX, template);
        while (m.find()) {
            String placeholder = m.group(1);
            boolean contentHasToBeUsedRaw = m.group(2) != null && m.group(4) != null;
            String variableKey = m.group(3);
            String placeholderValue = (String) parameters.get(variableKey);
            if (placeholderValue == null) {
                LOGGER.warning(String.format("Can not found '%s' value.", variableKey));
                placeholderValue = "";
            }
            String formattedValue = contentHasToBeUsedRaw ? placeholderValue : StringEscape.escapeHtml(placeholderValue);

            template = template.replace(placeholder, formattedValue);
        }

        return template;
    }

    @SuppressWarnings("unchecked")
    private String replaceForLoops(String template, Map<String, Object> parameters) {
        Matcher m = matcher(LOOPS_REGX, template);
        if (m.find()) {
            String matchedForLoop = m.group(1);
            String fieldName = m.group(2);
            String collectionName = m.group(3);
            String output = m.group(4).trim();

            StringBuilder forItemBuilder = new StringBuilder();

            List<HashMap<String, Object>> forItems = (List<HashMap<String, Object>>) parameters.get(collectionName);


            for (HashMap<String, Object> currentElement : forItems) {
                Map<String, Object> forKeyValuePairs = new HashMap<>();

                for (String key : currentElement.keySet()) {
                    String value = (String) currentElement.get(key);

                    forKeyValuePairs.put(fieldName + "." + key, value);
                }

                forItemBuilder.append(replacePlaceholders(output, forKeyValuePairs));
            }

            template = template.replace(matchedForLoop, forItemBuilder);
        }

        return template;
    }

    private String replaceIfStatements(String template, Map<String, Object> parameters) {
        Matcher m = matcher(IFTHEN_REGX, template);

        if (m.find()) {
            String ifStatement = m.group();

            Matcher ifGroups = matcher(IFELSE_REGX, template);

            boolean hasFoundTruthyIfStatement = false;

            while (ifGroups.find()) {
                String ifType = ifGroups.group(2);
                boolean isNegatedIf = ifGroups.group(3) != null;
                String ifVariableKey = ifGroups.group(4);
                String output = ifGroups.group(5);
                boolean ifTypeIsElse = ifType.equals("else");

                boolean ifRequirementIsTruthy = ifTypeIsElse || (Boolean) parameters.get(ifVariableKey);

                boolean evaluatesToTrue = ifTypeIsElse || isNegatedIf != ifRequirementIsTruthy;

                if (evaluatesToTrue) {
                    hasFoundTruthyIfStatement = true;
                    template = template.replace(ifStatement, replacePlaceholders(output, parameters));

                    break;
                }
            }

            if (!hasFoundTruthyIfStatement) {
                template = template.replace(ifStatement, "");
            }
        }

        return template;
    }

    private String getTemplateName(String name){
        return name.contains(".html") ? name : name.concat(".html");
    }

    private class ContentBlock {
        String wrapper;
        String content;
    }

    private interface ContentLocator {
        String get(String arg) throws IOException;
    }

    private class FileLocator implements ContentLocator {
        private String basePath;

        public FileLocator(String path){
            basePath = path;
        }

        @Override
        public String get(String arg) throws IOException {
            return getFileContent(Paths.get(basePath, arg));
        }

        private String getFileContent(Path path) throws IOException {
            return new String(cache.apply(path.toUri().toURL()));
        }
    }

    private class ResourceLocator implements ContentLocator {
        private String basePath;

        public ResourceLocator(String base){
            basePath = base;
        }

        @Override
        public String get(String arg) throws IOException {
            String resource = String.format("%s/%s", basePath, arg);
            URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
            if (null == url) {
                throw new IOException(String.format("Can not found '%s' template resource.", resource));
            }
            return new String(cache.apply(url));
        }
    }

    public static Matcher matcher(final String regex, final String template) {
        return Pattern.compile(regex).matcher(template);
    }
}
