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

import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.implementation.spring.bundle.api.ServiceInterface;

/**
 * First service with default priority 1
 */
@FrameworkComponent(services = ServiceInterface.class)
public class ServiceInterfaceImpl1 implements ServiceInterface {
    @Override
    public String doThing() {
        return "done";
    }
}
