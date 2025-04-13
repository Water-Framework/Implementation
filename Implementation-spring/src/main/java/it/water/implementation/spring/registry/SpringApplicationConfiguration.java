
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

package it.water.implementation.spring.registry;

import it.water.core.api.registry.ApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * @Author Aristide Cittadino.
 */
@Component()
public class SpringApplicationConfiguration implements ApplicationConfiguration {
    private static Logger log = LoggerFactory.getLogger(SpringApplicationConfiguration.class);
    Environment environment;
    Properties props;
    Properties copy;

    @Override
    public Properties getConfiguration() {
        return copy;
    }

    public void loadProperties() {
        if (props == null || copy == null) {
            props = new Properties();
            for (PropertySource<?> propertySource : ((AbstractEnvironment) environment).getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource) {
                    for (String propertyName : ((EnumerablePropertySource<?>) propertySource).getPropertyNames()) {
                        log.info(propertyName+" value {} "+environment.getProperty(propertyName));
                        props.put(propertyName, environment.getProperty(propertyName));
                    }
                }
            }
            copy = new Properties();
            copy.putAll(props);
        }
    }

    @Autowired
    public void setEnvironment(Environment environment) {
        log.debug("setting environment...");
        this.environment = environment;
        if (environment != null)
            this.loadProperties();
    }
}
