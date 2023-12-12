
/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.water.implementation.osgi.bundle;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.rest.RestApiManager;
import it.water.core.bundle.RuntimeInitializer;
import it.water.core.registry.model.ComponentConfigurationFactory;
import it.water.implementation.osgi.interceptors.ServiceHooks;
import it.water.implementation.osgi.registry.OsgiComponentRegistry;
import org.osgi.framework.*;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author Aristide Cittadino.
 */
public class OsgiDistributionInitializer<T> extends RuntimeInitializer<T, ServiceRegistration<T>> implements BundleActivator {
    private static final Logger log = LoggerFactory.getLogger(OsgiDistributionInitializer.class);

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        log.debug("Starting Water Core OSGi activator...");
        this.setupApplicationProperties();
        this.startFrameworkComponents();
        this.startInterceptors(bundleContext);
        this.startRestApis();
        log.debug("Water Core Activation Completed!");
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        log.debug("Stopping Base OSGi activator...");
        stopRestApis();
        log.debug("Unregistering framework components...");
        unregisterFrameworkComponents();

    }

    private void setupApplicationProperties() {
        ApplicationProperties waterApplicationProperties = new OsgiApplicationProperties();
        ComponentConfiguration configuration = ComponentConfigurationFactory.createNewComponentPropertyFactory().build();
        waterApplicationProperties.setup();
        this.getComponentRegistry().registerComponent(ApplicationProperties.class, waterApplicationProperties, configuration);
    }

    private void startFrameworkComponents() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            this.initializeFrameworkComponents();
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    private void startRestApis() {
        log.debug("Registering rest apis...");
        this.initializeRestApis();
    }

    private void stopRestApis() {
        BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        ServiceReference<RestApiManager> restApiManagerServiceReference = ctx.getServiceReference(RestApiManager.class);
        if (restApiManagerServiceReference != null) {
            RestApiManager restApiManager = ctx.getService(restApiManagerServiceReference);
            try {
                restApiManager.stopRestApiServer();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    private void unregisterFrameworkComponents() {
        getRegisteredServices().forEach(registeredService -> {
            try {
                log.debug("Unregistering component {}", registeredService.getComponent().getClass().getName());
                ServiceRegistration<?> registration = registeredService.getRegistration();
                registration.unregister();
            } catch (Exception t) {
                log.error(t.getMessage(), t);
            }
        });
    }

    private void startInterceptors(BundleContext context) {
        log.debug("Registering interceptors...");
        try {
            ServiceHooks sv = new ServiceHooks(context, getComponentRegistry());
            context.registerService(new String[]{FindHook.class.getName(), EventListenerHook.class.getName()},
                    sv, null);
        } catch (Exception t) {
            log.error(t.getMessage(), t);
        }
    }

    @Override
    protected ComponentRegistry getComponentRegistry() {
        return OsgiComponentRegistry.getInstance();
    }

    @Override
    protected Runtime getRuntime() {
        return new OsgiRuntime();
    }

}
