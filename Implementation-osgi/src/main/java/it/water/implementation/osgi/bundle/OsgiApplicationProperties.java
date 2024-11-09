/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.water.implementation.osgi.bundle;

import it.water.core.api.bundle.ApplicationProperties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;

/**
 * @Author Aristide Cittadino
 * OSGi Application Properties, registered manually as component at startup
 */
public class OsgiApplicationProperties implements ApplicationProperties {
    private static Logger logger = LoggerFactory.getLogger(OsgiApplicationProperties.class);

    private static final String APPLICATION_DEFAULT_CFG = "etc/it.water.application";
    private Properties properties;

    @Override
    public void setup() {
        this.properties = new Properties();
        File cfgPath = new File(APPLICATION_DEFAULT_CFG);
        loadProperties(cfgPath);
    }

    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    @Override
    public void loadProperties(File file) {
        if (getConfigurationAdmin() != null) {
            try {
                Configuration configuration = getConfigurationAdmin().getConfiguration(file.getName());
                Properties tmp = new Properties();
                Iterator<String> it = configuration.getProperties().keys().asIterator();
                while (it.hasNext()) {
                    String key = it.next();
                    Object val = configuration.getProperties().get(key);
                    tmp.put(key, val);
                }
                loadProperties(tmp);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void loadProperties(Properties props) {
        this.properties.putAll(props);
    }

    @Override
    public void unloadProperties(File file) {
        try {
            Configuration configuration = getConfigurationAdmin().getConfiguration(file.getName());
            Iterator<String> it = configuration.getProperties().keys().asIterator();
            while (it.hasNext()) {
                String key = it.next();
                this.properties.remove(key);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void unloadProperties(Properties props) {
        props.keySet().forEach(key -> this.properties.remove(key));
    }


    private ConfigurationAdmin getConfigurationAdmin() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        return bundleContext.getService(bundleContext.getServiceReference(ConfigurationAdmin.class));
    }
}
