/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.apache.pulsar.config;

import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Properties;


public class PropertyLoader {
    private static Logger logger = Logger.getLogger(PropertyLoader.class);


    public static void loadEnvironmentValues(Properties properties) {
        Enumeration keys = properties.propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String env_safe_key = key.replace(".", "_");
            env_safe_key = env_safe_key.replace("-", "_");
            String envValue = System.getenv(env_safe_key.toUpperCase());
            String systemPropValue = System.getProperty(key.toUpperCase());
            String oldValue = properties.getProperty(key);
            System.out.println(key + ":" + oldValue);
            if (envValue != null) {
                properties.setProperty(key, envValue);
                logger.debug("Setting java property for key:" + key + " ,value:" + envValue + " ,oldValue:" + oldValue);
            } else if (systemPropValue != null) {
                properties.setProperty(key, systemPropValue);
                logger.debug("Setting java property for key:" + key + " ,value:" + systemPropValue + " ,oldValue:"
                        + oldValue);
            }
        }
    }

}
