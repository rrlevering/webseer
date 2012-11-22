package org.webseer.java;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface FunctionDef {
	String description() default "";

	String[] keywords() default {};

	ImportLibrary[] libraries() default {};
}
