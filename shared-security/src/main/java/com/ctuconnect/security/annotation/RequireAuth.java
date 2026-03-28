package com.ctuconnect.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require authentication for accessing endpoints
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {
    /**
     * Required roles to access the endpoint
     */
    String[] roles() default {};

    /**
     * Whether the user can only access their own data
     */
    boolean selfOnly() default false;
}
