
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

import it.water.core.api.registry.ComponentRegistry;
import it.water.implementation.osgi.interceptors.ServiceHooks;
import it.water.implementation.osgi.registry.OsgiComponentRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author Aristide Cittadino.
 */
public class OsgiDistributionInitializer<T> extends WaterBundleActivator<T> {
    private static final Logger log = LoggerFactory.getLogger(OsgiDistributionInitializer.class);

    public OsgiDistributionInitializer() {
        super(true);
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
        log.info("Starting interceptors...");
        this.startInterceptors(bundleContext);
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

}
