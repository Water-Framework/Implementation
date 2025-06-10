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

package it.water.implementation.osgi.interceptors;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.implementation.osgi.util.OSGiUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.osgi.framework.ServiceEvent.*;


/**
 * @Author Aristide Cittadino
 * This Hook creates a proxy instance for every BaseService.
 * Then real implementation are obfuscated by these hooks.
 * Proxy instances are keept in sync with original ones with EventListenerHook
 */
public class ServiceHooks implements EventListenerHook, FindHook {
    private static Logger log = LoggerFactory.getLogger(ServiceHooks.class.getName());
    private static final String OBJECT_CLASS = "objectClass";

    private ComponentRegistry componentRegistry;

    public ServiceHooks(BundleContext bc, ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
        //getting already registered bundles
        this.loadAlreadyRegisteredServices(bc.getBundles());
    }

    /**
     * @param alreadyRegistereSerivces already registered bundle before this hook is started
     * @Aristide Cittadino
     * It can be possible that some Service are loaded before this hook is loaded ex. AuthenticationSystemApi
     * This method is invoked at Hook startup and analyze already registered bundles , searching for Service to proxy
     */
    private void loadAlreadyRegisteredServices(Bundle[] alreadyRegistereSerivces) {
        for (Bundle b : alreadyRegistereSerivces) {
            registerWaterServiceFromBundle(b);
        }
    }

    private <S extends Service> void registerWaterServiceFromBundle(Bundle b) {
        ServiceReference<?>[] references = b.getRegisteredServices();
        if (references != null) {
            for (ServiceReference<?> serviceReference : references) {
                String[] propertyKeys = serviceReference.getPropertyKeys();
                Dictionary<String, Object> properties = buildProps(propertyKeys, serviceReference);
                Bundle bundle = serviceReference.getBundle();
                if (bundle != null && isWaterService(serviceReference)) {
                    @SuppressWarnings("unchecked")
                    ServiceReference<S> wtfServiceRef = (ServiceReference<S>) serviceReference;
                    String[] interfaces = (String[]) wtfServiceRef.getProperty(OBJECT_CLASS);
                    S service = bundle.getBundleContext().getService(wtfServiceRef);
                    OSGiUtil.registerProxyService(bundle, interfaces, properties, this.getClass().getClassLoader(), service, this.componentRegistry);
                }
            }
        }
    }

    /**
     * Author Aristide Cittadino
     * This method is overridden in order to obfuscate  Service which are not "proxy".
     * The goal is to mask real implementation in OSGi and expose only proxied services.
     * With this approach each  Service can be wrapped by proxy and it can be customized
     * with Interceptors
     *
     * @param bc          Bundle Context
     * @param name        Component Name
     * @param filter      OSGi filter
     * @param allServices boolean to catch all services
     * @param references  found references
     */
    @Override
    public void find(BundleContext bc, String name, String filter, boolean allServices, @SuppressWarnings("rawtypes") Collection references) {
        try {
            Iterator<?> iterator = references.iterator();
            while (iterator.hasNext()) {
                @SuppressWarnings("rawtypes")
                ServiceReference<?> sr = (ServiceReference) iterator.next();
                if (isWaterService(sr)) {
                    iterator.remove();
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * @param event
     * @param listeners
     * @Author Aristide Cittadino
     * This method is overridden in order to intercept  Service Component declaration and to register automatically proxied versions of that services.
     */
    @Override
    public void event(ServiceEvent event, Map<BundleContext, Collection<ListenerHook.ListenerInfo>> listeners) {
        try {
            final ServiceReference<?> serviceReference = event.getServiceReference();
            String[] propertyKeys = serviceReference.getPropertyKeys();
            Dictionary<String, Object> properties = buildProps(propertyKeys, serviceReference);
            Bundle bundle = serviceReference.getBundle();
            if (isWaterService(serviceReference)) {
                //no bundle will receive updates from this one because it's not the proxied one
                listeners.clear();
                @SuppressWarnings("unchecked")
                ServiceReference<Service> wtfServiceRef = (ServiceReference<Service>) event.getServiceReference();
                String[] interfaces = (String[]) wtfServiceRef.getProperty(OBJECT_CLASS);
                switch (event.getType()) {
                    case REGISTERED: {
                        Service service = (Service) bundle.getBundleContext().getService(serviceReference);
                        if (service != null) {
                            OSGiUtil.registerProxyService(bundle, interfaces, properties, this.getClass().getClassLoader(), service, this.componentRegistry);
                        }
                        break;
                    }
                    case UNREGISTERING: {
                        OSGiUtil.unregisterProxyService(bundle, serviceReference.getClass());
                        break;
                    }
                    case MODIFIED, MODIFIED_ENDMATCH: {
                        Service s = (Service) bundle.getBundleContext().getService(serviceReference);
                        OSGiUtil.unregisterProxyService(bundle, serviceReference.getClass());
                        if (s != null) {
                            OSGiUtil.registerProxyService(bundle, interfaces, properties, this.getClass().getClassLoader(), s, this.componentRegistry);
                        }
                        break;
                    }
                    default: {
                        log.warn("Unrecognized event type {}", event.getType());
                    }
                }
            }
        } catch (Exception t) {
            log.error(t.getMessage(), t);
        }
    }

    /**
     * @param sr
     * @return true if the service reference represents an Serivce
     */
    private boolean isWaterService(ServiceReference<?> sr) {
        String[] interfaces = (String[]) sr.getProperty(OBJECT_CLASS);
        boolean isProxy = OSGiUtil.isWaterServiceProxyInstance(sr);
        return OSGiUtil.isWaterService(interfaces, isProxy, null);
    }

    /**
     * Copies all the pros of a service
     *
     * @param propertyKeys
     * @param sr
     * @return
     */
    private Dictionary<String, Object> buildProps(String[] propertyKeys, ServiceReference<?> sr) {
        Map<Object, Object> properties = new HashMap<>();
        for (String string : propertyKeys) {
            properties.put(string, sr.getProperty(string));
        }
        return OSGiUtil.toDictionary(properties);
    }
}
