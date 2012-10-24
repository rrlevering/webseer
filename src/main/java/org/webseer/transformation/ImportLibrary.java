package org.webseer.transformation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ImportLibrary {
	String group();
	String name();
	String version();
}
