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

import it.water.core.api.bundle.ApplicationProperties;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;
import java.util.regex.Matcher;

/**
 * PropertySource that resolves Water's ${env:VAR_NAME:-default} placeholder syntax
 * before returning values to Spring, preventing conflicts with Spring's own ${VAR:default} resolver.
 */
public class WaterPropertiesPropertySource extends PropertiesPropertySource {

    public WaterPropertiesPropertySource(String name, Properties source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        Object raw = super.getProperty(name);
        if (!(raw instanceof String rawStr)) return raw;
        Matcher matcher = ApplicationProperties.ENV_PATTERN.matcher(rawStr);
        if (!matcher.find()) return rawStr;
        matcher.reset();
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String defaultValue = matcher.group(3);
            String envValue = System.getenv(varName);
            if (envValue == null) envValue = defaultValue != null ? defaultValue : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(envValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}