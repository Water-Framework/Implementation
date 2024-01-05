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

import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.registry.model.ComponentConfigurationFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;


public class OsgiComponentRegistration<T> implements ComponentRegistration<T, ServiceRegistration<T>> {
    private ServiceRegistration<T> registration;
    private ServiceReference<T> serviceReference;
    private Dictionary<String, Object> properties;
    private Class<? extends T> registrationClass;

    public OsgiComponentRegistration(Class<? extends T> registrationClass, ServiceRegistration<T> registration) {
        this.registrationClass = registrationClass;
        this.serviceReference = registration.getReference();
        this.properties = this.serviceReference.getProperties();
        this.registration = registration;
    }

    @Override
    public T getComponent() {
        BundleContext ctx = serviceReference.getBundle().getBundleContext();
        return ctx.getService(serviceReference);
    }

    @Override
    public ComponentConfiguration getConfiguration() {
        return ComponentConfigurationFactory.createNewComponentPropertyFactory().fromStringDictionary(this.properties).build();
    }

    @Override
    public Class<? extends T> getRegistrationClass() {
        return registrationClass;
    }

    @Override
    public ServiceRegistration<T> getRegistration() {
        return registration;
    }

    public ServiceReference<T> getServiceReference() {
        return serviceReference;
    }
}
