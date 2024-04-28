
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

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.bundle.RuntimeInitializer;
import it.water.implementation.spring.registry.SpringComponentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;


/**
 * @Author Aristide Cittadino.
 * This class intializes springboot application with 2 different phases:
 * 1. Post Processor Bean Factory: in this phase only framework components are loaded. This happens because @Autowired fields are checked after this phase.
 * In order to make @FrameworkComponents working in spring environment we need to laode before the @Autowired checks. With this approach
 * @FrameworkComponents are injectable with @Autowired annotation.
 * 2. Context Refresh Event: after all components are loaded the final step is to load permissions,actions and eventually rest apis
 */
@Service
public class BaseSpringInitializer<T> extends RuntimeInitializer<T, String> implements BeanFactoryPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(BaseSpringInitializer.class);
    private SpringComponentRegistry componentRegistry;
    //run initialization just once
    private static boolean started = false;
    private static boolean initialized = false;

    @Override
    public synchronized void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!initialized) {
            log.debug("Registering components....");
            this.componentRegistry = new SpringComponentRegistry(beanFactory);
            this.initializeFrameworkComponents(true);
            initialized = true;
        }
    }

    /**
     * Method will be run only once.
     * We support the application context refreshed event and component initialization.
     * At the time of writing there's no need to execut this method multiple times
     */
    @EventListener
    public synchronized void applicationStartup(ContextRefreshedEvent event) {
        if (!started) {
            //forcing setting application context
            this.componentRegistry.setApplicationContext(event.getApplicationContext());
            this.activateComponents();
            log.info("################# Starting Water Framework #################");
            log.debug("Setting up actions and permissions....");
            this.initializeResourcePermissionsAndActions();
            log.debug("Registering rest APIs....");
            this.initializeRestApis();
            log.debug("################# Water Framework Application Setup Complete! #################");
            started = true;
        }
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

    /**
     * Spring needs just to register one bean then the spring registry will register one instance per implemented interface.
     * @return
     */
    @Override
    protected boolean registerMultiInterfaceComponents() {
        return false;
    }
}
