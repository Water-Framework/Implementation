
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

package it.water.implementation.osgi.test;

import it.water.implementation.osgi.util.test.karaf.WaterTestConfigurationBuilder;
import org.ops4j.pax.exam.ConfigurationFactory;
import org.ops4j.pax.exam.Option;

public class WaterFrameworkTestConfiguration implements ConfigurationFactory {
    @Override
    public Option[] createConfiguration() {
        return WaterTestConfigurationBuilder.createStandardConfiguration()
                .withCodeCoverage("it.water.implementation.osgi")
                .withDebug("5005", false)
                .build();
    }
}
