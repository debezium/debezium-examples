/*
 *
 * Copyright (c) 2018 Nutanix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Yuvaraj L
 */
package io.debezium.examples.apache.pulsar.config;
import java.util.Enumeration;
import java.util.Properties;



public class PropertyLoader {


    public static void loadEnvironmentValues(Properties properties) {
        System.out.println("=================================");
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
                System.out.println("Setting java property for key:" + key + " ,value:" + envValue + " ,oldValue:" + oldValue);
            } else if (systemPropValue != null) {
                properties.setProperty(key, systemPropValue);
                System.out.println("Setting java property for key:" + key + " ,value:" + systemPropValue + " ,oldValue:"
                        + oldValue);
            }
        }
    }

}
