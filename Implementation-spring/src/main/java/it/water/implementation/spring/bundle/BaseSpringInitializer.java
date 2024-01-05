
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

package it.water.implementation.spring.bundle;

import it.water.core.api.bundle.Runtime;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.bundle.RuntimeInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;


/**
 * @Author Aristide Cittadino.
 */
public class BaseSpringInitializer<T> extends RuntimeInitializer<T, String> {
    private static final Logger log = LoggerFactory.getLogger(BaseSpringInitializer.class);
    private ComponentRegistry componentRegistry;

    @EventListener
    public void applicationStartup(ContextRefreshedEvent event) {
        log.info("################# Starting Water Framework #################");
        log.debug("Registering components....");
        this.initializeFrameworkComponents(false, false);
        log.debug("Registering rest APIs....");
        this.initializeRestApis();
        log.debug("################# Water Framework Application Setup Complete! #################");
    }

    /**
     * Creating specific ComponentRegistry injecting all required spring objects
     *
     * @return
     */
    @Override
    public ComponentRegistry getComponentRegistry() {
        return this.componentRegistry;
    }

    @Override
    protected Runtime getRuntime() {
        return new SpringRuntime();
    }

    @Autowired
    public void setComponentRegistry(ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }
}
