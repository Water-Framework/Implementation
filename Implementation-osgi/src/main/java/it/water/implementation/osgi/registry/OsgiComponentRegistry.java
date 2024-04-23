
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

import it.water.core.api.interceptors.OnActivate;
import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.api.service.Service;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.registry.model.ComponentConfigurationFactory;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import it.water.implementation.osgi.interceptors.OsgiServiceInterceptor;
import it.water.implementation.osgi.util.OSGiUtil;
import it.water.implementation.osgi.util.filter.OSGiComponentFilterBuilder;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @Author Aristide Cittadino
 * No need to register as component since the base initializer do it automatically.
 */
public class OsgiComponentRegistry implements ComponentRegistry {
    private static final Logger log = LoggerFactory.getLogger(OsgiComponentRegistry.class);
    private static final String PRIORITY = "it.water.component.priority";
    public static final OSGiComponentFilterBuilder componentFilterBuilder = new OSGiComponentFilterBuilder();
    private static OsgiComponentRegistry instance;
    private Map<Class<?>, ServiceRegistration<?>> registrations = new HashMap<>();

    private OsgiComponentRegistry() {
    }

    public static OsgiComponentRegistry getInstance() {
        if (instance == null) {
            instance = new OsgiComponentRegistry();
        }
        return instance;
    }

    @Override
    public <T> T findComponent(Class<T> componentClass, ComponentFilter filter) {
        List<T> components = findComponents(componentClass, filter);
        if (components != null && !components.isEmpty()) {
            if (components.size() > 1)
                log.warn("Multiple components found for type: {}, returning the one with highest priority ", componentClass.getName());
            return components.get(0);
        }
        throw new NoComponentRegistryFoundException("No components found for :" + componentClass.getName() + " with filter: " + filter);
    }

    @Override
    public <T> List<T> findComponents(Class<T> componentClass, ComponentFilter filter) {
        try {
            BundleContext bundleContext = getBundleContext(componentClass);
            String filterStr = (filter != null) ? filter.getFilter() : null;
            Collection<ServiceReference<T>> serviceReferences = bundleContext.getServiceReferences(componentClass, filterStr);
            List<ServiceReference<T>> orderedServiceReferences = new ArrayList<>(serviceReferences);
            Collections.sort(orderedServiceReferences, (sr1, sr2) -> {
                //we don't know if standard components have been registered
                //so we put the lowest priority
                int prioritySr1 = (sr1.getProperty(PRIORITY) != null) ? (Integer) sr1.getProperty(PRIORITY) : -1;
                int prioritySr2 = (sr2.getProperty(PRIORITY) != null) ? (Integer) sr2.getProperty(PRIORITY) : -1;
                if (prioritySr1 > prioritySr2)
                    return -1;
                else if (prioritySr1 == prioritySr2)
                    return 0;
                return 1;
            });
            return orderedServiceReferences.stream().map(bundleContext::getService).collect(Collectors.toList());
        } catch (InvalidSyntaxException e) {
            throw new WaterRuntimeException(e.getMessage());
        } catch (Exception e) {
            log.error("Unknown error while trying to find component {},please check exported and imported packages!", componentClass.getName());
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public <T, K> ComponentRegistration<T, K> registerComponent(Class<? extends T> componentClass, T component, ComponentConfiguration configuration) {
        BundleContext context = getBundleContext(component.getClass());
        if (configuration == null) {
            configuration = ComponentConfigurationFactory.createNewComponentPropertyFactory().build();
        }
        //in OSGi priority is added as a property
        //default priority is 1
        configuration.addProperty(PRIORITY, configuration.getPriority());
        //adding inferred classes to the registration so they can be read at runtime from osgi properties.
        String[] componentClassesNames = calculateComponentClasses(componentClass, component);
        ServiceRegistration<T> registration = null;
        //if the current instance implements directly or indirectly water service interface, registry will automatically register a proxy instance
        if (OSGiUtil.isWaterService(componentClassesNames, false, component)) {
            registration = (ServiceRegistration<T>) OSGiUtil.registerProxyService(context.getBundle(), componentClassesNames, configuration.getConfigurationAsDictionary(), component.getClass().getClassLoader(), (Service) component, this);
        } else {
            registration = (ServiceRegistration<T>) context.registerService(componentClassesNames, component, configuration.getConfigurationAsDictionary());
        }

        this.invokeLifecycleMethod(OnActivate.class, component.getClass(), context.getService(registration.getReference()));
        ComponentRegistration<T, ServiceRegistration<T>> componentRegistration = new OsgiComponentRegistration<>(componentClass, registration);
        //registrations are associated with specific classes of each component
        registrations.put(component.getClass(), registration);
        return (ComponentRegistration<T, K>) componentRegistration;
    }

    private <T> String[] calculateComponentClasses(Class<? extends T> componentClass, T component) {
        Set<String> componentClassesNames = new HashSet<>();
        componentClassesNames.add(componentClass.getName());
        //get recursive interfaces exposed by the whole hierarchy using the concrete class
        //in order to find other interfaces not directly exposed
        getRecursiveInterfaces(component.getClass(), componentClassesNames);
        return componentClassesNames.toArray(new String[componentClassesNames.size()]);
    }

    private void getRecursiveInterfaces(Class<?> currentClass, Set<String> componentClassesNames) {
        for (Class<?> anInterface : currentClass.getInterfaces()) {
            componentClassesNames.add(anInterface.getName());
        }
    }

    @Override
    public boolean unregisterComponent(ComponentRegistration registration) {
        return unregisterComponent(registration.getRegistrationClass(), registration.getComponent());
    }

    @Override
    public <T> boolean unregisterComponent(Class<T> componentClass, T component) {
        //retrieving registration for specific component class which is the implementation class
        Class<?> classToFind = component.getClass();
        if (Proxy.isProxyClass(classToFind) && Proxy.getInvocationHandler(component) instanceof OsgiServiceInterceptor) {
            classToFind = ((OsgiServiceInterceptor) Proxy.getInvocationHandler(component)).getOriginalConcreteClass();
        }
        //removing normal components
        if (registrations.containsKey(classToFind)) {
            registrations.get(classToFind).unregister();
            registrations.remove(classToFind);
            return true;
        }
        this.invokeLifecycleMethod(OnDeactivate.class, classToFind, component);
        return false;
    }

    @Override
    public ComponentFilterBuilder getComponentFilterBuilder() {
        return componentFilterBuilder;
    }

    private <T> BundleContext getBundleContext(Class<T> componentClass) {
        return FrameworkUtil.getBundle(componentClass).getBundleContext();
    }
}
