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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

/**
 * @Author Aristide Cittadino
 * OSGi Application Properties, registered manually as component at startup
 */
public class OsgiApplicationProperties implements ApplicationProperties {
    private static Logger logger = LoggerFactory.getLogger(OsgiApplicationProperties.class);

    private static final String APPLICATION_DEFAULT_CFG = "etc/it.water.application";
    private static final String DEFAULT_CFG_PID = "it.water.application";
    private static final String DEFAULT_PROPERTY_FILE = DEFAULT_CFG_PID + ".properties";

    private Properties properties;

    @Override
    public void setup() {
        this.properties = new Properties();
        File cfgPath = new File(APPLICATION_DEFAULT_CFG);
        loadProperties(cfgPath);
    }

    @Override
    public Object getProperty(String key) {
        if (containsKey(key))
            return this.resolvePropertyValue(getConfigurationAdminProperties().get(key).toString());
        return null;
    }

    @Override
    public boolean containsKey(String key) {
        return getConfigurationAdminProperties().get(key) != null;
    }

    public void loadBundleProperties(BundleContext bundleContext) {
        URL cfgResource = bundleContext.getBundle().getResource(DEFAULT_PROPERTY_FILE);
        if (cfgResource != null) {
            Properties props = new Properties();
            try (InputStream is = cfgResource.openStream()) {
                props.load(is);
            } catch (IOException e) {
                logger.error("Failed to load properties from " + cfgResource, e);
            }
            //adding only properties not already defined
            props.keySet().forEach(key -> {
                Object value = props.get(key);
                if (!this.properties.contains(key)) {
                    this.properties.put(key, value);
                } else
                    logger.warn("WATER PROPERTY CONFLICT! Key {} with value {} from module {}. will be discarded", key, value, bundleContext.getBundle().getSymbolicName());
            });
            updateOsgiConfigurationManager();
        }
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
        this.updateOsgiConfigurationManager();
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
            this.updateOsgiConfigurationManager();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void unloadProperties(Properties props) {
        props.keySet().forEach(key -> this.properties.remove(key));
        updateOsgiConfigurationManager();
    }

    private ConfigurationAdmin getConfigurationAdmin() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        return bundleContext.getService(bundleContext.getServiceReference(ConfigurationAdmin.class));
    }

    private Dictionary<? extends Object, Object> getConfigurationAdminProperties() {
        try {
            return getConfigurationAdmin().getConfiguration(DEFAULT_CFG_PID).getProperties();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new Properties();
    }

    private void updateOsgiConfigurationManager() {
        //Updating the whole it.water.application.cfg with all properties merged
        Dictionary<String, Object> dictionary = new Hashtable<>();
        this.properties.forEach((key, value) -> dictionary.put(key.toString(), value));
        try {
            Configuration configuration = getConfigurationAdmin().getConfiguration(DEFAULT_CFG_PID);
            //save config osgi
            configuration.update(dictionary);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
