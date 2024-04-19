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
package it.water.implementation.osgi.util;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.implementation.osgi.interceptors.OsgiServiceInterceptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.*;

public class OSGiUtil {
    private static Logger log = LoggerFactory.getLogger(OSGiUtil.class);
    public static final String WATER_OSGI_PROPS_PROXY = "it.water.core.api.interceptors.isProxy";

    private OSGiUtil() {
    }

    /**
     * Maps properties to dictionary
     *
     * @param props
     * @return
     */
    public static Dictionary<String, Object> toDictionary(Map<Object, Object> props) {
        Map<String, Object> map = new HashMap<>();
        Iterator<?> it = props.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            map.put(key, props.get(key));
        }
        return new Hashtable<>(map);
    }

    /**
     * @param sr
     * @return true if the service reference is a proxy instance
     */
    public static boolean isWaterServiceProxyInstance(ServiceReference<?> sr) {
        String waterProxyProp = sr != null && sr.getProperty(WATER_OSGI_PROPS_PROXY) != null ?sr.getProperty(WATER_OSGI_PROPS_PROXY).toString():null;
        return waterProxyProp != null && !waterProxyProp.isBlank() && Boolean.parseBoolean(waterProxyProp);
    }

    /**
     * Check wether interfaces or instance represents a water service.
     * Basically if the instance or interfaces inherit water Service interface.
     *
     * @param interfaces
     * @param isProxy
     * @param instance
     * @return
     */
    public static boolean isWaterService(String[] interfaces, boolean isProxy, Object instance) {
        if (instance != null && Service.class.isAssignableFrom(instance.getClass()))
            return !isProxy;

        for (String clazz : interfaces) {
            try {
                Class<?> c = Class.forName(clazz);
                boolean isWtfService = Service.class.isAssignableFrom(c);
                if (isWtfService) {
                    return !isProxy;
                }
            } catch (Exception ex) {
                log.debug(" class definition {} not found, because: {}, skipping HYT proxy for this service", clazz, ex.getMessage());
            }
        }
        return false;
    }

    /**
     * Register the proxied version of an Service
     *
     * @param bundleSource
     * @param interfacesToRegister
     * @param componentConfiguration
     * @param cl
     * @param service
     */
    public static <S extends Service> ServiceRegistration<S> registerProxyService(Bundle bundleSource, String[] interfacesToRegister, Dictionary<String, Object> componentConfiguration, ClassLoader cl, S service, ComponentRegistry componentRegistry) {
        try {
            OsgiServiceInterceptor<S> osgiServiceInterceptor = new OsgiServiceInterceptor<>(service, componentRegistry);
            Class<?>[] toClass = toClass(interfacesToRegister, bundleSource);
            componentConfiguration.put(WATER_OSGI_PROPS_PROXY, true);
            Object waterServiceProxy = Proxy.newProxyInstance(cl, toClass, osgiServiceInterceptor);
            ServiceRegistration<S> registration = (ServiceRegistration<S>) bundleSource.getBundleContext().registerService(toString(toClass), waterServiceProxy, componentConfiguration);
            osgiServiceInterceptor.setRegistration(registration);
            return registration;
        } catch (NoClassDefFoundError e) {
            log.error("NoClassDefFoundError by {} in source bundle {}", cl, bundleSource, e);
        } catch (Exception t) {
            log.error(t.getMessage(), t);
        }
        return null;
    }

    /**
     * Unregister the proxied version of an Service
     *
     * @param bundleSource
     * @param serviceClass
     * @param <S>
     */
    public static <S extends ServiceReference> void unregisterProxyService(Bundle bundleSource, Class<S> serviceClass) {
        try {
            Collection<ServiceReference<S>> references = bundleSource.getBundleContext().getServiceReferences(serviceClass, "(" + WATER_OSGI_PROPS_PROXY + "=true)");
            references.stream().forEach(ref -> {
                OsgiServiceInterceptor<?> proxy = (OsgiServiceInterceptor) bundleSource.getBundleContext().getService(ref);
                proxy.getRegistration().unregister();
                log.debug("Unregistering {} as Service ", proxy.getRegistration().getReference().getBundle().getSymbolicName());
            });
        } catch (InvalidSyntaxException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Converts array of string, representing class names, to array of classes objects
     *
     * @param interfaces
     * @param bl
     * @return
     */
    private static Class<?>[] toClass(String[] interfaces, Bundle bl) {
        List<Class<?>> names = new ArrayList<>();

        for (String clazz : interfaces) {
            try {
                Class<?> classObj = bl.loadClass(clazz);
                if (classObj != null) names.add(classObj);
            } catch (ClassNotFoundException ex) {
                log.debug("Class {} not found in Bundle {}", clazz, bl.getSymbolicName());
            }
        }
        Class<?>[] returnNames = new Class<?>[names.size()];
        return names.toArray(returnNames);
    }

    /**
     * Converts classes array to array of string
     *
     * @param interfaces
     * @return
     */
    private static String[] toString(Class<?>[] interfaces) {
        String[] names = new String[interfaces.length];
        int i = 0;
        for (Class<?> clazz : interfaces) {
            names[i++] = clazz.getName();
        }
        return names;
    }
}
