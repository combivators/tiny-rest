package net.tiny.ws.mvc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.tiny.config.Config;
import net.tiny.config.Configuration;
import net.tiny.config.ConfigurationHandler;

/**
 * 消息源
 *
 */
@Config("messages")
public class MessageResources {

    private static Logger LOGGER = Logger.getLogger(MessageResources.class.getName());

    /** 资源包缓存*/
    private static ConcurrentHashMap<Locale, List<ResourceBundle>> BUNDLE_CACHE =
            new ConcurrentHashMap<Locale, List<ResourceBundle>>();

    /** 消息源文件名前缀 */
    private List<String> basenames = new ArrayList<>();

    private boolean useCodeAsDefaultMessage = false;

    private boolean cache = true;

    //private String locale = Locale.getDefault().toString();

    private Locale locale = null;

    public List<String>  getBasenames() {
        return basenames;
    }

    public void setBasenames(List<String> basenames) {
        this.basenames = basenames;
    }

    public boolean isCache() {
        return this.cache;
    }

    public void setCache(boolean enable) {
        this.cache = enable;
    }

    public String getLocaleString() {
        return String.format("%s_%s", locale.getLanguage(), locale.getCountry());
    }

    /*
    public void setLocaleString(String localeString) {
        this.localeString = localeString;
    }
*/
    public Locale getLocale() {
        /*
        if(null == locale) {
            String[] values = this.localeString.split("_");
            locale = new Locale(values[0], values[1]);
        }
        */
        return locale;
    }

    public void setLocale(Locale locale) {
        //this.localeString = locale.getLanguage() + "_" + locale.getCountry();
        this.locale = locale;
    }

    public boolean getUseCodeAsDefaultMessage() {
        return this.useCodeAsDefaultMessage;
    }

    public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
        this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
    }

    public String getMessage(String defaultMessage, Locale locale, Object... arguments) {
        return formatMessage(defaultMessage, locale, arguments);
    }

    public String getMessage(String defaultMessage, Object... arguments) {
        return formatMessage(defaultMessage, getLocale(), arguments);
    }


    public List<ResourceBundle> getResourceBundles(Locale locale) {
        if(cache && BUNDLE_CACHE.containsKey(locale)) {
            return BUNDLE_CACHE.get(locale);
        }
        List<ResourceBundle> bundles = new ArrayList<ResourceBundle>();
        ResourceBundle.Control resourceController =
                ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES);
        for (String basename : this.basenames) {
            try {
                ResourceBundle bundle =
                           ResourceBundle.getBundle(basename, locale, resourceController);
                if (null != bundle) {
                    bundles.add(bundle);
                    if (LOGGER.isLoggable(Level.CONFIG))
                        LOGGER.config(String.format("[REST] '%1$s_%2$s.properties' has bean loaded.", basename, locale.toLanguageTag()));
                } else {
                    LOGGER.warning(String.format("[REST] Not found '%1$s_%2$s.properties'", basename, locale.toLanguageTag()));
                }
            } catch (MissingResourceException ex) {
                continue;
            }
        }
        if(cache && !bundles.isEmpty()) {
            BUNDLE_CACHE.put(locale, bundles);
        }
        return bundles;
    }

    protected String formatMessage(String code, Locale locale, Object... arguments) {
        List<ResourceBundle> bundles = getResourceBundles(locale);
        for (ResourceBundle bundle : bundles) {
            try {
                String message = bundle.getString(code);
                if(null != arguments && arguments.length > 0) {
                    MessageFormat messageFormat = new MessageFormat(message, locale);
                    synchronized (messageFormat) {
                        return messageFormat.format(arguments);
                    }
                } else {
                    return message;
                }
            } catch (MissingResourceException ex) {
                continue;
            }
        }
        if(useCodeAsDefaultMessage) {
            return code;
        } else {
            return null;
        }
    }

    public final static String DEFAULT_CONFIG_FILE = "messages.properties";

    private static MessageResources instance = null;

    public static MessageResources getInstance() {
        if(null == instance) {
            instance = valueOf(DEFAULT_CONFIG_FILE);
        }
        return instance;
    }

    public static MessageResources valueOf(String resource) {
        final ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource(resource);
        handler.parse();
        final Configuration config = handler.getConfiguration();
        return config.getAs(MessageResources.class);
    }
}
