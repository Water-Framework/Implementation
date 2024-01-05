
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

package it.water.implementation.osgi.registry;

import it.water.core.api.registry.ApplicationConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class OsgiApplicationConfiguration implements ApplicationConfiguration {
    private static Logger log = LoggerFactory.getLogger(OsgiApplicationConfiguration.class);
    Properties props;
    Properties copy;

    public void start() {
        this.loadProperties();
    }

    @Override
    public Properties getConfiguration() {
        return copy;
    }

    public void loadProperties() {
        if (props == null) {
            BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
            ServiceReference<?> configurationAdminReference = context
                    .getServiceReference(ConfigurationAdmin.class.getName());

            if (configurationAdminReference != null) {
                ConfigurationAdmin confAdmin = (ConfigurationAdmin) context
                        .getService(configurationAdminReference);
                try {
                    Configuration configuration = confAdmin
                            .getConfiguration("it.water.application");
                    if (configuration != null && configuration.getProperties() != null) {
                        Dictionary<String, Object> dict = configuration.getProperties();
                        List<String> keys = Collections.list(dict.keys());
                        Map<String, Object> dictCopy = keys.stream()
                                .collect(Collectors.toMap(Function.identity(), dict::get));
                        props = new Properties();
                        props.putAll(dictCopy);
                        log.debug("Loaded properties For Water: {}", props);
                    }
                } catch (IOException e) {
                    log.error(
                            "Impossible to find it.water.application.cfg, please create it!", e);
                }
            } else {
                log.error(
                        "Impossible to find it.water.application.cfg, please create it!");
            }

        }
        copy = new Properties();
        copy.putAll(props);
    }
}
