package net.tiny.ws.rs;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import net.tiny.service.ClassFinder;
import net.tiny.service.Patterns;

/**
 * REST（Representational State Transfer） 表述性状态传递应用设置
 * @see javax.ws.rs.core.Application
 */
@ApplicationPath("/rest")
public class RestApplication extends Application {

    private static Logger LOGGER = Logger.getLogger(RestApplication.class.getName());
    static final String PROPERTY_KEY = RestApplication.class.getName();

    private String loggingLevel   = "fine";
    private String pattern = "!java.*, !javax.*, !com.sun.*";
    private Set<Object> singletons = new HashSet<Object>();
    private ClassFinder classFinder;

    public void setLoggingLevel(String level) {
    	this.loggingLevel = level;
    }

    public void setPattern(String pattern) {
    	this.pattern = pattern;
    }

    /**
     * @see Application#getClasses()
     */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> restClasses = findAllRestClasses();
        Collections.unmodifiableSet(restClasses);
        Collections.synchronizedSet(restClasses);
        return restClasses;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    public ClassFinder getClassFinder() {
    	if (classFinder == null) {
            final String include = System.getProperty("javax.ws.rs.scan.packages.include");
            final String exclude = System.getProperty("javax.ws.rs.scan.packages.exclude");
            final String logging = System.getProperty("javax.ws.rs.logging.level");
            final Patterns patterns;
            if(include == null && exclude== null ) { //Default patterns
                patterns = Patterns.valueOf(pattern);
            } else {
                patterns = new Patterns(include, exclude);
            }
            if (logging != null) {
            	loggingLevel = logging;
            }

        	classFinder =
                    new ClassFinder(Thread.currentThread().getContextClassLoader(),
                            false, new RestClassFilter(patterns));
    	}
    	return classFinder;
    }

    private Set<Class<?>> findAllRestClasses() {
        try {
        	final ClassFinder finder = getClassFinder();
            final Level level = Level.parse(loggingLevel.toUpperCase());
            ClassFinder.setLoggingLevel(level);
            List<Class<?>> rests = finder.findAnnotatedClasses(javax.ws.rs.Path.class);
            LOGGER.info(String.format("[REST] Registered %d REST classe(s) with pattern '%s'", rests.size(), pattern.toString()));
            for (Class<?> r : rests) {
            	LOGGER.log(level, String.format("[REST] '%s' registered", r.getName()));
            }

            Set<Class<?>> classSet = new HashSet<>(rests);
            List<Class<?>> providers = finder.findAnnotatedClasses(javax.ws.rs.ext.Provider.class);
            LOGGER.info(String.format("[REST] Found %d Providers classe(s) with pattern '%s'",
            		providers.size(), pattern.toString()));
            for (Class<?> p : providers) {
            	LOGGER.log(level, String.format("[REST] Provider '%s' registered", p.getSimpleName()));
            }
            return classSet;
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    static class RestClassFilter implements ClassFinder.Filter {
        private final Patterns patterns;

        public RestClassFilter(Patterns patterns) {
            this.patterns = patterns;
        }

        @Override
        public boolean isTarget(Class<?> targetClass) {
            return true;
        }

        @Override
        public boolean isTarget(String className) {
            return this.patterns.vaild(className);
        }
    }
}
