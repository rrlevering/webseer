package org.webseer.util;

import java.io.File;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * This is the configuration loader for webseer. This uses Apache's very open configuration loader.
 * 
 * @author Ryan Levering
 */
public class WebseerConfiguration {

	private final static String KEY = WebseerConfiguration.class.getName();

	/**
	 * This is used for web applications that use webseer to read configuration properties from the init parameters in
	 * the web.xml file.
	 * 
	 * @param application the application to get the configuration from
	 * @return the created webseer Configuration
	 */
	public static Configuration getConfiguration(ServletContext application) {
		Configuration conf = (Configuration) application.getAttribute(KEY);
		if (conf == null) {
			conf = getConfiguration();
			Enumeration<?> e = application.getInitParameterNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				conf.setProperty(name, application.getInitParameter(name));
			}
			application.setAttribute(KEY, conf);
		}
		return conf;
	}

	private static Configuration config;

	public static final Configuration getConfiguration() {
		if (config == null) {
			PropertiesConfiguration allConfig = new PropertiesConfiguration();
			String[] files = new String[] { "webseer.properties", "secret.properties" };
			for (String file : files) {
				try {
					allConfig.load(file);
				} catch (ConfigurationException e) {
					// Keep trying to load the others
					System.err.println("Unable to load configuration file " + file);
				}
			}
			config = allConfig;
		}
		return config;
	}

	private static final String[] knownClasspaths = new String[] { "file:(.*)/target/classes",
			"file:(.*)/build/classes", "file:(.*)/WEB-INF/classes" };

	public static File getWebseerRoot() {
		String classPath = WebseerConfiguration.class.getResource("WebseerConfiguration.class").toString();

		String root = null;
		for (String regex : knownClasspaths) {
			Pattern compiled = Pattern.compile(regex);
			Matcher matcher = compiled.matcher(classPath);
			if (matcher.find()) {
				root = matcher.group(1);
			}
		}

		return new File(root);
	}
}
