
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

package it.water.implementation.spring.util.filter;

import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.registry.filter.ComponentDefaultPropertyFilter;

@FrameworkComponent
public class SpringComponentFilterBuilder implements ComponentFilterBuilder {
    public static final SpringComponentFilterImplementation SPRING_COMPONENT_FILTER_IMPLEMENTATION = new SpringComponentFilterImplementation();

    public ComponentFilter createFilter(String name, String value) {
        return new ComponentDefaultPropertyFilter(name, value, SPRING_COMPONENT_FILTER_IMPLEMENTATION);
    }
}
