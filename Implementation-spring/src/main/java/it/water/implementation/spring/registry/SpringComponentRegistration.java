
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

import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.registry.model.ComponentConfigurationFactory;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;


/**
 * @param <T>
 * @Author Aristide Cittadino.
 */
public class SpringComponentRegistration<T> implements ComponentRegistration<T, String> {
    private String beanName;
    private T component;
    private Properties properties;

    private Class<? extends T> registrationClass;

    public SpringComponentRegistration(Class<? extends T> registrationClass, String beanName, T component) {
        this.beanName = beanName;
        this.component = component;
        this.properties = new Properties();
        this.registrationClass = registrationClass;
    }

    @Override
    public T getComponent() {
        return component;
    }

    @Override
    public ComponentConfiguration getConfiguration() {
        Map<String, Object> map = new HashMap<>();
        this.properties.forEach((name, val) -> map.put(name.toString(), val));
        return ComponentConfigurationFactory.createNewComponentPropertyFactory().fromGenericDictionary(new Hashtable<>(map)).build();
    }

    @Override
    public Class<? extends T> getRegistrationClass() {
        return registrationClass;
    }

    @Override
    public String getRegistration() {
        return beanName;
    }
}
