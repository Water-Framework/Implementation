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
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.rest.RestApiManager;
import it.water.core.bundle.RuntimeInitializer;
import it.water.core.registry.model.ComponentConfigurationFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T>
 * @Author Aristide Cittadino
 * Generic Bundle Activator that can be used for modules which must be deployed inside OSGi container.
 */
public class WaterBundleActivator<T> extends RuntimeInitializer<T, ServiceRegistration<T>> implements org.osgi.framework.BundleActivator {
    private static final Logger log = LoggerFactory.getLogger(WaterBundleActivator.class);

    //Boolean used only to register a bundle which intialize the whole framework - Core module
    //For any other module it should be kept false
    private boolean newRuntime;

    //Using bundle context to retrieve the current class loader
    private BundleContext bundleContext;

    /**
     * Only one bundle should have a bundle activator with newRuntime = true.
     * This means that bundle is the waster core bundle.
     *
     * @param newRuntime
     */
    protected WaterBundleActivator(boolean newRuntime) {
        this.newRuntime = newRuntime;
    }

    public WaterBundleActivator() {
        this(false);
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        log.debug("Starting {} ...", bundleContext.getBundle().getSymbolicName());
        //loading @FrameworkComponents
        this.startFrameworkComponents();
        //Initializing permissions
        this.initializeResourcePermissionsAndActions();
        //Register rest api if any
        this.startRestApis();
        log.debug(" Bundle {} \" - Activation Completed!\"", bundleContext.getBundle().getSymbolicName());
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        log.debug("Stopping Base OSGi activator...");
        //Stop rest apis if any
        stopRestApis();
        log.debug("Unregistering framework components...");
        unregisterFrameworkComponents();
        this.bundleContext = null;
    }

    /**
     * Retrieves component registry from the OSGi context
     *
     * @return
     */
    @Override
    protected ComponentRegistry getComponentRegistry() {
        BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        ServiceReference<ComponentRegistry> sr = ctx.getServiceReference(ComponentRegistry.class);
        return ctx.getService(sr);
    }

    /**
     * Creates a new application properties component, reading property file.
     */
    protected void setupApplicationProperties() {
        ApplicationProperties waterApplicationProperties = new OsgiApplicationProperties();
        ComponentConfiguration configuration = ComponentConfigurationFactory.createNewComponentPropertyFactory().build();
        waterApplicationProperties.setup();
        this.getComponentRegistry().registerComponent(ApplicationProperties.class, waterApplicationProperties, configuration);
    }

    /**
     * Loads @FrameworkComponent. newRuntime is false, and it should be kept false.
     */
    protected void startFrameworkComponents() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getCurrentClassLoader());
        try {
            this.initializeFrameworkComponents(newRuntime);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    /**
     * Loading rest APIs
     */
    protected void startRestApis() {
        log.debug("Registering rest apis...");
        this.initializeRestApis();
    }

    /**
     * Stops Rest APIs
     */
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


    /**
     * Unregister @FrameworkComponents
     */
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

    @Override
    protected ClassLoader getCurrentClassLoader() {
        return bundleContext.getBundle().adapt(BundleWiring.class).getClassLoader();
    }
}
