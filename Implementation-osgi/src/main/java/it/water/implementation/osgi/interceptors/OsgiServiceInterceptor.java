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
import it.water.core.interceptors.WaterAbstractInterceptor;
import lombok.Getter;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * @Author Aristide Cittadino
 * Implementation of Service proxy.
 * This class will wrap every Service. It implements the logic to intercept methods invocation.
 * It gives the possibility to the developer to customize pre-invocation or post-invocation logic on every Service.
 */
public class OsgiServiceInterceptor<S extends Service> extends WaterAbstractInterceptor<S> implements InvocationHandler, Serializable {
    private static Logger log = LoggerFactory.getLogger(OsgiServiceInterceptor.class.getName());
    private transient ServiceRegistration<S> registration;
    @Getter
    private transient ComponentRegistry componentRegistry;

    public OsgiServiceInterceptor(S service, ComponentRegistry componentRegistry) {
        super(service);
        this.componentRegistry = componentRegistry;
    }

    public ServiceRegistration<S> getRegistration() {
        return registration;
    }

    public void setRegistration(ServiceRegistration<S> registration) {
        this.registration = registration;
    }

    /**
     * Each invocation is wrapped between "intercept before" and "intercept after".
     *
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            executeInterceptorBeforeMethod(getService(), method, args);
            Object invoke = method.invoke(getService(), args);
            executeInterceptorAfterMethod(getService(), method, args, invoke);
            return invoke;
        } catch (IllegalAccessException | NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            throw new IllegalAccessException("Error while intercept method call for: " + proxy.getClass().getName() + " " + method.getName());
        } catch (InvocationTargetException e) {
            log.error("Invocation on proxy failed, please check exceptions!");
            throw e.getTargetException();
        }
    }

    @Override
    protected ComponentRegistry getComponentsRegistry() {
        return this.componentRegistry;
    }
}
