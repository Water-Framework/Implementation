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

package it.water.implementation.spring;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.bundle.PropertiesNames;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.registry.model.ComponentConfigurationFactory;
import it.water.core.security.model.principal.UserPrincipal;
import it.water.implementation.spring.annotations.EnableWaterFramework;
import it.water.implementation.spring.bundle.api.ServiceInterface;
import it.water.implementation.spring.bundle.service.*;
import it.water.implementation.spring.interceptors.SpringServiceInterceptor;
import it.water.implementation.spring.security.SpringSecurityContext;
import it.water.implementation.spring.util.filter.SpringComponentFilterBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest()
@ActiveProfiles("test")
@ContextConfiguration(classes = TestConfiguration.class)
@EnableWaterFramework
class SpringApplicationTest {
    private static final String FIELD1_NAME = "field1";
    private static final String FIELD2_NAME = "field2";
    private static final String FIELD_VALUE = "field1New";
    @Autowired
    ComponentRegistry waterComponentRegistry;
    @Autowired
    SpringServiceInterceptor springServiceInterceptor;
    @Autowired
    ServiceSample serviceTest;
    @Autowired
    ApplicationProperties waterApplicationProperties;
    @Autowired
    SpringSystemServiceApi springSystemServiceApi;

    @Test
    void initSpringApplication() {
        assertNotNull(waterComponentRegistry);
        assertNotNull(springServiceInterceptor);
    }

    /**
     * This test checks if interceptors work correctly
     * since in serviceTest we inject a core componet using WaterInject annotation
     * which is managed by interceptors.
     * So if it is <> null then interceptors works correctly, generically.
     */
    @Test
    void testInterceptors() {
        assertNotNull(serviceTest);
        assertNotNull(springSystemServiceApi);
        assertNotNull(serviceTest.getRegistry());
    }

    /**
     * This test checks wether the component registry orders or not all registered componente using the component registry
     */
    @Test
    void testPriorityAndUnregistering() {
        List<ServiceInterface> services = waterComponentRegistry.findComponents(ServiceInterface.class, null);
        Assertions.assertEquals(3, services.size());
        ServiceInterface sr = waterComponentRegistry.findComponent(ServiceInterface.class, null);
        //find component should return the one with highest priority
        Assertions.assertTrue(sr instanceof ServiceInterfaceImpl3);
        //testing injection in framework components
        Assertions.assertNotNull(((ServiceInterfaceImpl3) sr).getComponentRegistry());
        waterComponentRegistry.unregisterComponent(ServiceInterface.class, services.get(0));
        services = waterComponentRegistry.findComponents(ServiceInterface.class, null);
        Assertions.assertEquals(2, services.size());
        Assertions.assertInstanceOf(ServiceInterfaceImpl2.class, services.get(0));
    }

    @Test
    void testValidation() {
        TestResource resource = new TestResource();
        Assertions.assertThrows(ValidationException.class, () -> springSystemServiceApi.elaborateResource(resource));
    }


    @Test
    void checkLoadedProperties() {
        assertNotNull(waterApplicationProperties);
        assertEquals("true", waterApplicationProperties.getProperty(PropertiesNames.HYPERIOT_TEST_MODE));
    }

    @Test
    void testComponentFilter() {
        SpringComponentFilterBuilder componentFilterBuilder = new SpringComponentFilterBuilder();
        ComponentFilter filter = componentFilterBuilder.createFilter("filter", "value");
        ServiceInterface serviceInterface = waterComponentRegistry.findComponent(ServiceInterface.class, filter);
        Assertions.assertNotNull(serviceInterface);
        Assertions.assertEquals("FILTERED BEAN!", serviceInterface.doThing());
        ComponentFilter andFilter = filter.and(componentFilterBuilder.createFilter("filter1", "value1"));
        Assertions.assertEquals("(&(filter=value)(filter1=value1))", andFilter.getFilter());
        Assertions.assertEquals("(!(&(filter=value)(filter1=value1)))", andFilter.not().getFilter());
        ComponentFilter orFilter = filter.or(componentFilterBuilder.createFilter("filter1", "value1"));
        Assertions.assertEquals("(|(filter=value)(filter1=value1))", orFilter.getFilter());
        Assertions.assertEquals("(!(|(filter=value)(filter1=value1)))", orFilter.not().getFilter());
        Assertions.assertEquals("(!(filter=value))", filter.not().getFilter());
    }

    @Test
    void testWaterComponentRegistry() {
        ServiceInterface customComponent = new ServiceInterfaceImpl3();
        ComponentRegistration<ServiceInterface, String> registration = this.waterComponentRegistry.registerComponent(ServiceInterface.class, customComponent, ComponentConfigurationFactory.createNewComponentPropertyFactory().withPriority(4).build());
        Assertions.assertNotNull(registration);
        Assertions.assertNotNull(registration.getConfiguration());
        Assertions.assertNotNull(this.waterComponentRegistry.getComponentFilterBuilder());
        Assertions.assertNotNull(this.waterComponentRegistry.findEntitySystemApi(FakeEntity.class.getName()));
        Assertions.assertNotNull(this.waterComponentRegistry.findEntityRepository(FakeEntity.class.getName()));
        Assertions.assertNull(this.waterComponentRegistry.findEntityExtensionRepository(FakeEntity.class));
        Assertions.assertEquals(ServiceInterface.class, registration.getRegistrationClass());
        Assertions.assertDoesNotThrow(() -> this.waterComponentRegistry.unregisterComponent(registration));
    }

    @Test
    void testSpringApplicationProperties() {
        File customPropFile = new File("src/spring/test/resources/custom-props.properties");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.waterApplicationProperties.loadProperties(customPropFile));
        Properties customProps2 = new Properties();
        customProps2.put("customFromCode", "value");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.waterApplicationProperties.loadProperties(customProps2));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.waterApplicationProperties.unloadProperties(customPropFile));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.waterApplicationProperties.unloadProperties(customProps2));
    }

    @Test
    void testSpringSecurityContext() {
        Set<Principal> principals = new HashSet<>();
        principals.add(new UserPrincipal("user", false, 1, "entity"));
        SpringSecurityContext springSecurityContext = new SpringSecurityContext(principals);
        Assertions.assertFalse(springSecurityContext.isSecure());
        Assertions.assertEquals("default", springSecurityContext.getAuthenticationScheme());
        SpringSecurityContext springSecurityContext1 = new SpringSecurityContext(principals, "customImplementation");
        Assertions.assertNotNull(springSecurityContext1);
    }
}
