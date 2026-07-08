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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for WaterPropertiesPropertySource.
 * No Spring context is started — the class is instantiated directly.
 *
 * Branches covered:
 *  - getProperty: raw value is NOT a String  → returns raw unchanged            (line 37 false branch)
 *  - getProperty: String without placeholder → returns rawStr unchanged          (line 39 true branch)
 *  - getProperty: String with ${env:VAR:-default}, env var absent → uses default (lines 42-48 true branch, line 46 both sub-branches)
 *  - getProperty: String with placeholder and NO default group (group(3)==null)  → falls back to ""
 *  - getProperty: String with multiple placeholders in one value
 *  - getProperty: key not present in Properties → returns null (non-String raw)
 */
class WaterPropertiesPropertySourceTest {

    private static final String SOURCE_NAME = "test-source";

    private Properties props;

    @BeforeEach
    void setUp() {
        props = new Properties();
    }

    // -----------------------------------------------------------------------
    // Branch: raw value is not a String (e.g. Integer stored in Properties)
    // -----------------------------------------------------------------------
    @Test
    void testGetProperty_nonStringValue_returnsRawUnchanged() {
        props.put("intProp", 42);
        WaterPropertiesPropertySource source = new WaterPropertiesPropertySource(SOURCE_NAME, props);

        Object result = source.getProperty("intProp");

        assertEquals(42, result);
    }

    // -----------------------------------------------------------------------
    // Branch: raw is null (key absent) — super.getProperty returns null,
    //         null instanceof String is false, so raw (null) is returned
    // -----------------------------------------------------------------------
    @Test
    void testGetProperty_keyAbsent_returnsNull() {
        WaterPropertiesPropertySource source = new WaterPropertiesPropertySource(SOURCE_NAME, props);

        Object result = source.getProperty("nonExistentKey");

        assertNull(result);
    }

    // -----------------------------------------------------------------------
    // Branch: String value with NO placeholder → returned unchanged
    // -----------------------------------------------------------------------
    @Test
    void testGetProperty_stringWithoutPlaceholder_returnsUnchanged() {
        props.setProperty("plainProp", "simpleValue");
        WaterPropertiesPropertySource source = new WaterPropertiesPropertySource(SOURCE_NAME, props);

        Object result = source.getProperty("plainProp");

        assertEquals("simpleValue", result);
    }

    // -----------------------------------------------------------------------
    // Branch: placeholder present, env var NOT set, default provided → default used
    // -----------------------------------------------------------------------
    @Test
    void testGetProperty_placeholderWithDefault_envAbsent_usesDefault() {
        // ENV_PATTERN: \$\{env:([^:}]+)(:-([^}]+))?}
        // Syntax: ${env:SOME_UNLIKELY_VAR_12345678:-myDefaultValue}
        // This env var should never be set in the test environment.
        final String varName = "WATER_TEST_VAR_UNLIKELY_XYZ_12345678";
        final String defaultValue = "myDefaultValue";
        props.setProperty("myProp", "${env:" + varName + ":-" + defaultValue + "}");
        WaterPropertiesPropertySource source = new WaterPropertiesPropertySource(SOURCE_NAME, props);

        // Ensure the var is not set (we rely on the JVM not having this env var)
        String envVal = System.getenv(varName);
        // If somehow the env var IS set, we skip the assertion to avoid flakiness
        if (envVal == null) {
            Object result = source.getProperty("myProp");
            assertEquals(defaultValue, result);
        }
    }

    // -----------------------------------------------------------------------
    // Branch: placeholder present, env var NOT set, NO default group → falls back to ""
    // -----------------------------------------------------------------------
    @Test
    void testGetProperty_placeholderWithoutDefault_envAbsent_returnsEmpty() {
        final String varName = "WATER_TEST_VAR_UNLIKELY_NO_DEFAULT_9876";
        // No ":-default" part → group(3) will be null
        props.setProperty("myProp", "${env:" + varName + "}");
        WaterPropertiesPropertySource source = new WaterPropertiesPropertySource(SOURCE_NAME, props);

        String envVal = System.getenv(varName);
        if (envVal == null) {
            Object result = source.getProperty("myProp");
            assertEquals("", result);
        }
    }

    // -----------------------------------------------------------------------
    // ENV_PATTERN (\$\{env:([^:}]+)(:-([^}]+))?}) requires the default, when present,
    // to have at least one character: the ":-}" (empty default) form does NOT match,
    // so the value is returned unchanged (matcher.find() == false branch).
    // -----------------------------------------------------------------------
    @Test
    void testGetProperty_placeholderWithEmptyDefault_notMatched_returnsUnchanged() {
        final String varName = "WATER_TEST_VAR_UNLIKELY_EMPTY_DEFAULT_4321";
        final String raw = "${env:" + varName + ":-}";
        props.setProperty("myProp", raw);
        WaterPropertiesPropertySource source = new WaterPropertiesPropertySource(SOURCE_NAME, props);

        String envVal = System.getenv(varName);
        if (envVal == null) {
            Object result = source.getProperty("myProp");
            assertEquals(raw, result);
        }
    }

    // -----------------------------------------------------------------------
    // Branch: multiple placeholders in the same string value — while loop iterates > 1 time
    // -----------------------------------------------------------------------
    @Test
    void testGetProperty_multiplePlaceholders_bothReplaced() {
        final String var1 = "WATER_TEST_VAR_MULTI_A_11111";
        final String var2 = "WATER_TEST_VAR_MULTI_B_22222";
        props.setProperty("multiProp",
                "prefix-${env:" + var1 + ":-alpha}-middle-${env:" + var2 + ":-beta}-suffix");
        WaterPropertiesPropertySource source = new WaterPropertiesPropertySource(SOURCE_NAME, props);

        String envVal1 = System.getenv(var1);
        String envVal2 = System.getenv(var2);
        if (envVal1 == null && envVal2 == null) {
            Object result = source.getProperty("multiProp");
            assertEquals("prefix-alpha-middle-beta-suffix", result);
        }
    }

    // -----------------------------------------------------------------------
    // Branch: mixed content — placeholder surrounded by literal text
    // -----------------------------------------------------------------------
    @Test
    void testGetProperty_placeholderInMiddleOfString_literalPartsPreserved() {
        final String varName = "WATER_TEST_VAR_MID_55555";
        props.setProperty("mixedProp", "hello-${env:" + varName + ":-world}-!");
        WaterPropertiesPropertySource source = new WaterPropertiesPropertySource(SOURCE_NAME, props);

        String envVal = System.getenv(varName);
        if (envVal == null) {
            Object result = source.getProperty("mixedProp");
            assertEquals("hello-world-!", result);
        }
    }

    // -----------------------------------------------------------------------
    // Verify constructor wires name correctly (basic smoke test)
    // -----------------------------------------------------------------------
    @Test
    void testConstructor_nameIsPreserved() {
        WaterPropertiesPropertySource source = new WaterPropertiesPropertySource("my-source-name", props);
        assertEquals("my-source-name", source.getName());
    }
}
