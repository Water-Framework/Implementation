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
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.service.rest.RestApiManager;
import it.water.core.bundle.RuntimeInitializer;
import it.water.core.registry.model.ComponentConfigurationFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WaterBundleActivator<T> extends RuntimeInitializer<T, ServiceRegistration<T>> implements org.osgi.framework.BundleActivator {
    private static final Logger log = LoggerFactory.getLogger(WaterBundleActivator.class);
    boolean newRuntime;

    /**
     * Only one bundle should have a bundle activator with newRuntime = true.
     * This means that bundle is the waster core bundle.
     *
     * @param newRuntime
     */
    protected WaterBundleActivator(boolean newRuntime) {
        this.newRuntime = newRuntime;
    }

    protected WaterBundleActivator() {
        this(false);
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        log.debug("Starting {} ...", bundleContext.getBundle().getSymbolicName());
        this.setupApplicationProperties();
        this.startFrameworkComponents();
        this.initializeResourcePermissionsAndActions();
        this.startRestApis();
        log.debug(" Bundle {} \" - Activation Completed!\"", bundleContext.getBundle().getSymbolicName());
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        log.debug("Stopping Base OSGi activator...");
        stopRestApis();
        log.debug("Unregistering framework components...");
        unregisterFrameworkComponents();

    }

    protected void setupApplicationProperties() {
        ApplicationProperties waterApplicationProperties = new OsgiApplicationProperties();
        ComponentConfiguration configuration = ComponentConfigurationFactory.createNewComponentPropertyFactory().build();
        waterApplicationProperties.setup();
        this.getComponentRegistry().registerComponent(ApplicationProperties.class, waterApplicationProperties, configuration);
    }

    protected void startFrameworkComponents() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            this.initializeFrameworkComponents(newRuntime);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    protected void startRestApis() {
        log.debug("Registering rest apis...");
        this.initializeRestApis();
    }

    protected void stopRestApis() {
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

    protected void unregisterFrameworkComponents() {
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

}
