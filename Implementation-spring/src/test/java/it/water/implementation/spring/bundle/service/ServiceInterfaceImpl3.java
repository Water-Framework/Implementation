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
package it.water.implementation.spring.bundle.service;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.implementation.spring.bundle.api.ServiceInterface;
import lombok.Getter;
import lombok.Setter;

/**
 * Second service which should have higher prioroity than first
 */
@FrameworkComponent(services = ServiceInterface.class, priority = 3, properties = {
        "filter=value"
})
public class ServiceInterfaceImpl3 implements ServiceInterface {
    @Inject
    @Setter
    @Getter
    private ComponentRegistry componentRegistry;

    @Setter
    @Getter
    private String filter;

    @Override
    public String doThing() {
        return "FILTERED BEAN!";
    }
}
