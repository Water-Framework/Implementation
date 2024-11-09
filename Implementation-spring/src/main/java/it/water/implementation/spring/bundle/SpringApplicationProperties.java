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
package it.water.implementation.spring.bundle;

import it.water.core.api.bundle.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;
import java.util.Properties;

@Configuration
public class SpringApplicationProperties implements ApplicationProperties {
    private Environment environment;

    @Autowired
    public SpringApplicationProperties(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setup() {
        //Do nothing since properties are injected inside environemnt object
    }

    @Override
    public Object getProperty(String key) {
        return environment.getProperty(key);
    }

    @Override
    public boolean containsKey(String key) {
        return environment.containsProperty(key);
    }

    @Override
    public void loadProperties(File file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadProperties(Properties props) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unloadProperties(File file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unloadProperties(Properties props) {
        throw new UnsupportedOperationException();
    }
}
