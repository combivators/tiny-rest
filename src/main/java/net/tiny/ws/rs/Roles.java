package net.tiny.ws.rs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate the access roles
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Roles {

    /** Only system administrator can access */
	public static final String ADMIN = "admin";

    /** Accessible by system administrator and system operator */
	public static final String OPERATOR = "operator";

    /** Accessible by system administrator, system operator, viewer */
	public static final String READER = "reader";

	/** Not accessible */
    public static final String NONE = "none";

    public String[] value() default {Roles.NONE};
}
