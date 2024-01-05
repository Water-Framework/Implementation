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

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.bundle.PropertiesNames;
import it.water.core.interceptors.annotations.implementation.WaterComponentsInjector;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.registry.model.ComponentConfigurationFactory;
import it.water.implementation.osgi.test.bundle.ResourceSystemApi;
import it.water.implementation.osgi.test.bundle.ServiceInterface;
import it.water.implementation.osgi.test.bundle.ServiceInterfaceImpl2;
import it.water.implementation.osgi.test.bundle.TestResource;
import it.water.implementation.osgi.util.filter.OSGiComponentFilterBuilder;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import java.util.List;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class WaterFrameworkOSGiTest extends KarafTestSupport {

    //force global configuration
    @Override
    public Option[] config() {
        return null;
    }

    @Test
    public void waterFrameworkShouldBeInstalled() {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class, 0);
        String features = executeCommand("feature:list -i");
        assertContains("water-core-features  ", features);
        String datasource = executeCommand("jdbc:ds-list");
        assertContains("water", datasource);
    }

    /**
     * This test checks if interceptors work correctly
     * since in serviceTest we inject a core componet using WaterInject annotation
     * which is managed by interceptors.
     * So if it is <> null then interceptors works correctly, generically.
     */
    @Test
    public void testComponentRegistration() {
        ComponentRegistry registry = getOsgiService(ComponentRegistry.class);
        ResourceSystemApi resourceSystemApi = getOsgiService(ResourceSystemApi.class);
        WaterComponentsInjector injector = getOsgiService(WaterComponentsInjector.class);
        Assert.assertNotNull(registry);
        Assert.assertNotNull(resourceSystemApi);
        Assert.assertNotNull(injector);
    }

    /**
     * This test checks if interceptors work correctly
     * since in serviceTest we inject a core componet using Inject annotation
     * which is managed by interceptors.
     * So if it is <> null then interceptors works correctly, generically.
     */
    @Test
    public void testInterceptors() {
        ResourceSystemApi systemService = getOsgiService(ResourceSystemApi.class);
        Assert.assertNotNull(systemService.getComponentRegistry());
    }

    /**
     * This test checks wether the component registry orders or not all registered componente using the component registry
     */
    @Test
    public void testPriority() {
        ComponentRegistry waterComponentRegistry = getOsgiService(ComponentRegistry.class);
        List<ServiceInterface> services = waterComponentRegistry.findComponents(ServiceInterface.class, null);
        Assert.assertEquals(4, services.size());
        ServiceInterface sr = waterComponentRegistry.findComponent(ServiceInterface.class, null);
        //find component should return the one with highest priority
        Assert.assertEquals("done with priority", sr.doThing());
        waterComponentRegistry.unregisterComponent(ServiceInterface.class, services.get(0));
        services = waterComponentRegistry.findComponents(ServiceInterface.class, null);
        Assert.assertEquals(3, services.size());
    }

    @Test
    public void checkLoadedProperties() {
        ApplicationProperties waterApplicationProperties = getOsgiService(ApplicationProperties.class);
        Assert.assertNotNull(waterApplicationProperties);
        Assert.assertEquals("true", waterApplicationProperties.getProperty(PropertiesNames.HYPERIOT_TEST_MODE));
    }

    @Test
    public void testComponentFilter() {
        OSGiComponentFilterBuilder componentFilterBuilder = new OSGiComponentFilterBuilder();
        ComponentRegistry waterComponentRegistry = getOsgiService(ComponentRegistry.class);
        ComponentFilter filter = componentFilterBuilder.createFilter("filter", "value");
        ServiceInterface serviceInterface = waterComponentRegistry.findComponent(ServiceInterface.class, filter);
        Assert.assertNotNull(serviceInterface);
        Assert.assertEquals("FILTERED BEAN!", serviceInterface.doThing());
        ComponentFilter andFilter = filter.and(componentFilterBuilder.createFilter("filter1", "value1"));
        Assert.assertEquals("(&(filter=value)(filter1=value1))", andFilter.getFilter());
        Assert.assertEquals("(!(&(filter=value)(filter1=value1)))", andFilter.not().getFilter());
        ComponentFilter orFilter = filter.or(componentFilterBuilder.createFilter("filter1", "value1"));
        Assert.assertEquals("(|(filter=value)(filter1=value1))", orFilter.getFilter());
        Assert.assertEquals("(!(|(filter=value)(filter1=value1)))", orFilter.not().getFilter());
        Assert.assertEquals("(!(filter=value))", filter.not().getFilter());
    }

    @Test
    public void testEntityValidation() {
        String maliutiousField = "<script>alert('ciao')</script>";
        TestResource testResource = new TestResource();
        testResource.setField1(maliutiousField);
        testResource.setField2("field2");
        ResourceSystemApi waterTestEntitySystemApi = getOsgiService(ResourceSystemApi.class);
        //using save method of wtf base repository
        ValidationException ex = null;
        try {
            waterTestEntitySystemApi.validateResource(testResource);
        } catch (ValidationException e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
    }

    @Test
    public void testComponentRegistry() {
        ServiceInterface customComponent = new ServiceInterfaceImpl2();
        ComponentRegistry waterComponentRegistry = getOsgiService(ComponentRegistry.class);
        ComponentRegistration<ServiceInterface, String> registration = waterComponentRegistry.registerComponent(ServiceInterface.class, customComponent, ComponentConfigurationFactory.createNewComponentPropertyFactory().withPriority(4).build());
        Assert.assertNotNull(registration);
        Assert.assertNotNull(registration.getConfiguration());
        Assert.assertEquals(ServiceInterface.class, registration.getRegistrationClass());
        waterComponentRegistry.unregisterComponent(registration);
    }
}
