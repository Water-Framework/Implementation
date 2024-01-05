
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

package it.water.implementation.osgi.util.test.karaf;


public class WaterTestConfigurationBuilder {
    //TODO generate this variables from gradle automatically based on versions.properties
    private static final String HYT_VERSION = "3.0.0";
    private static final String KARAF_VERSION = "4.4.2";

    private WaterTestConfigurationBuilder() {
    }

    public static WaterTestConfiguration createStandardConfiguration(String testSuiteName) {
        return new WaterTestConfiguration(getKarafVersion(), getWaterRuntimeVersion(), testSuiteName);
    }

    public static WaterTestConfiguration createStandardConfiguration() {
        return new WaterTestConfiguration(getKarafVersion(), getWaterRuntimeVersion());
    }

    public static String getWaterRuntimeVersion() {
        return HYT_VERSION;
    }

    public static String getKarafVersion() {
        return KARAF_VERSION;
    }

}
