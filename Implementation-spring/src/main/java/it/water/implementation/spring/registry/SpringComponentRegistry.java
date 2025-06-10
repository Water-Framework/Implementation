
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;

import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.service.BaseEntitySystemApi;
import it.water.core.registry.AbstractComponentRegistry;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import it.water.implementation.spring.util.filter.SpringComponentFilterBuilder;
import lombok.Setter;


/**
 * @Author Aristide Cittadino.
 * No need to register as component since the base initializer do it automatically.
 */
public class SpringComponentRegistry extends AbstractComponentRegistry {
    private Logger log = LoggerFactory.getLogger(SpringComponentRegistry.class);
    @Setter
    private ApplicationContext applicationContext;
    private ConfigurableListableBeanFactory configurableBeanFactory;
    public static final SpringComponentFilterBuilder componentFilterBuilder = new SpringComponentFilterBuilder();

    public SpringComponentRegistry(ConfigurableListableBeanFactory configurableBeanFactory) {
        this.configurableBeanFactory = configurableBeanFactory;
    }

    @Override
    public <T> List<T> findComponents(Class<T> componentClass, ComponentFilter filter) {
        //filtering through ComponentFilter "matches" method
        Map<String, T> components = filterComponents(applicationContext.getBeansOfType(componentClass), filter);
        Collection<T> orderedByPriority = (new TreeMap<String, T>(components)).descendingMap().values();
        return new ArrayList<>(orderedByPriority);
    }

    @Override
    public <T> T findComponent(Class<T> componentClass, ComponentFilter filter) {
        List<T> componentsList = this.findComponents(componentClass, filter);
        if (!componentsList.isEmpty()) {
            if (componentsList.size() > 1)
                log.debug("Multiple components found for type: {}, returning the one with highest priority ", componentClass.getName());
            return componentsList.get(0);
        }
        throw new NoComponentRegistryFoundException("No components found for :" + componentClass.getName() + " with filter: " + filter);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, K> ComponentRegistration<T, K> registerComponent(Class<? extends T> componentClass, T component, ComponentConfiguration configuration) {
        String beanName = createBeanName(componentClass, component.getClass(), configuration);
        BeanDefinitionRegistry beanDefinitionRegistry = ((BeanDefinitionRegistry) configurableBeanFactory);
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(componentClass)
                .setLazyInit(false)
                .setScope(BeanDefinition.SCOPE_SINGLETON);
        beanDefinitionBuilder.getRawBeanDefinition().setInstanceSupplier(() -> component);
        beanDefinitionBuilder.setPrimary(configuration.isPrimary());
        //Adding all configured bean properties to the bean definition
        configuration.getConfiguration().forEach((name, value) -> beanDefinitionBuilder.addPropertyValue(name.toString(), value));
        beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
        ComponentRegistration<T, String> registration = new SpringComponentRegistration<>(componentClass, beanName, component);
        return (ComponentRegistration<T, K>) registration;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> boolean unregisterComponent(ComponentRegistration<T, ?> registration) {
        @SuppressWarnings("rawtypes")
        SpringComponentRegistration<String> springComponentRegistration = (SpringComponentRegistration) registration;
        removeBean(springComponentRegistration.getRegistration(), (T) springComponentRegistration.getComponent());
        this.invokeLifecycleMethod(OnDeactivate.class, registration.getRegistrationClass(), registration.getComponent());
        return true;
    }

    @Override
    public <T> boolean unregisterComponent(Class<T> componentClass, T component) {
        Map<String, T> components = applicationContext.getBeansOfType(componentClass);
        Optional<String> componentOptional = components.keySet().stream().filter(key -> components.get(key).equals(component)).findAny();
        if (componentOptional.isPresent() && configurableBeanFactory.containsBean(componentOptional.get())) {
            removeBean(componentOptional.get(), component);
            this.invokeLifecycleMethod(OnDeactivate.class, component.getClass(), component);
            return true;
        }
        return false;
    }

    @Override
    public ComponentFilterBuilder getComponentFilterBuilder() {
        return componentFilterBuilder;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T extends BaseEntitySystemApi> T findEntitySystemApi(String entityClassName) {
        Map<String, BaseEntitySystemApi> services = applicationContext.getBeansOfType(BaseEntitySystemApi.class);
        Optional<BaseEntitySystemApi> optService = services.values().stream().filter(service -> service.getEntityType().getName().equals(entityClassName)).findAny();
        if (optService.isPresent()) {
            return (T) optService.get();
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T extends BaseRepository> T findEntityRepository(String entityClassName) {
        Map<String, BaseRepository> services = applicationContext.getBeansOfType(BaseRepository.class);
        Optional<BaseRepository> optService = services.values().stream().filter(service -> service.getEntityType().getName().equals(entityClassName)).findAny();
        if (optService.isPresent()) {
            return (T) optService.get();
        }
        return null;
    }

    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    private <T> void removeBean(String name, T bean) {
        BeanDefinitionRegistry beanDefinitionRegistry = ((BeanDefinitionRegistry) configurableBeanFactory);
        configurableBeanFactory.destroyBean(name, bean);
        beanDefinitionRegistry.removeBeanDefinition(name);
    }

    private <T> Map<String, T> filterComponents(Map<String, T> registeredComponents, ComponentFilter filter) {
        Map<String, T> foundComponents = new HashMap<>();
        //filter == null means no filter
        if (filter == null)
            return registeredComponents;
        BeanDefinitionRegistry beanDefinitionRegistry = ((BeanDefinitionRegistry) configurableBeanFactory);
        registeredComponents.keySet().forEach(key -> {
            Properties props = new Properties();
            PropertyValues propertyValues = beanDefinitionRegistry.getBeanDefinition(key).getPropertyValues();
            propertyValues.forEach(propertyValue -> props.put(propertyValue.getName(), propertyValue.getValue()));
            if (filter.matches(props))
                foundComponents.put(key, registeredComponents.get(key));
        });
        return foundComponents;
    }

    private static String createBeanName(Class<?> componentClass, Class<?> concreteComponentClass, ComponentConfiguration configuration) {
        StringBuilder sb = new StringBuilder();
        //avoding registering bean of same class cause registration exeception because of the same name
        //we use convention of beanClass-priority
        sb.append(componentClass.getName()).append(":").append(concreteComponentClass.getName()).append("-").append(configuration.getPriority());
        return getBeanName(sb.toString());
    }

    private static String getBeanName(String className) {
        char[] c = className.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }

}
