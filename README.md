# Implementation Module

## Module Goal
The Implementation module provides the core integration layer for the Water Framework with different runtime environments, specifically OSGi and Spring. It abstracts the underlying platform details, enabling Water components, services, interceptors, security, and configuration to work seamlessly in both OSGi and Spring-based applications. This module is essential for supporting Water's cross-platform, modular, and extensible architecture.

## Module Technical Characteristics

### Core Technologies
- **OSGi (Open Services Gateway initiative):** Dynamic module system for Java, used for modular runtime and service management.
- **Spring Framework:** Popular Java application framework for dependency injection, configuration, and modularity.
- **SLF4J, Lombok, BouncyCastle, Nimbus JOSE JWT:** Logging, code reduction, encryption, and JWT support.
- **Water Core Modules:** Integrates with Water's core, model, permission, registry, security, service, and validation modules.
- **Karaf, Pax Exam:** OSGi runtime and testing utilities.
- **Spring Boot:** For Spring-based integration and testing.

### Architecture Components

#### OSGi Implementation (`Implementation-osgi`)
- **Bundle Activation:** `WaterBundleActivator` manages OSGi bundle lifecycle and initialization.
- **Component Registry:** `OsgiComponentRegistry` and related classes manage Water components in OSGi.
- **Interceptors:** `OsgiServiceInterceptor` and `ServiceHooks` provide AOP/interceptor support for OSGi services.
- **Security:** `OsgiSecurityContext` manages security context in OSGi.
- **Application Properties:** `OsgiApplicationProperties` provides configuration management.
- **Utilities:** OSGi-specific utilities for filtering, registration, and configuration.
- **Distribution Initializer:** `OsgiDistributionInitializer` for OSGi-specific distribution setup.

#### Spring Implementation (`Implementation-spring`)
- **Spring Configuration:** `WaterSpringConfiguration` and `EnableWaterFramework` annotation for auto-configuring Water in Spring.
- **Component Registry:** `SpringComponentRegistry` and related classes manage Water components in Spring.
- **Interceptors:** `SpringServiceInterceptor` provides AOP/interceptor support for Spring beans.
- **Security:** `SpringSecurityContext` manages security context in Spring.
- **Application Properties:** `SpringApplicationProperties` for configuration management.
- **Utilities:** Spring-specific utilities for filtering, registration, and configuration.
- **Distribution Initializer:** `BaseSpringInitializer` for Spring-specific distribution setup.

### Key Features
- Unified Water component model for both OSGi and Spring
- Dynamic component registration, filtering, and prioritization
- Interceptor/AOP support for service methods
- Security context abstraction for both platforms
- Application property management and configuration
- Platform-specific distribution and initialization
- Comprehensive test suites for both OSGi and Spring

## Permission and Security
- **Security Context:** Both OSGi and Spring implementations provide a `SecurityContext` abstraction for managing authentication and authorization.
- **Component Registration:** Only authorized components can be registered/unregistered at runtime.
- **Validation:** Input validation and entity validation are enforced via Water's core validation mechanisms.
- **Interceptors:** Can be used to enforce security, logging, and auditing policies on service methods.
- **Tested Security:** Test suites validate security context, property loading, and component registration security.

## How to Use It

### 1. Module Import
Add the Implementation module and the desired submodule (OSGi or Spring) to your project dependencies:

```gradle
// For OSGi
implementation 'it.water.implementation:Implementation-osgi:${waterVersion}'

// For Spring
implementation 'it.water.implementation:Implementation-spring:${waterVersion}'
```

### 2. OSGi Usage
- Deploy the OSGi bundle in a compatible OSGi container (e.g., Apache Karaf).
- Water components and services will be managed by the OSGi component registry.
- Use OSGi-specific APIs for component registration, filtering, and lifecycle management.
- Application properties are managed via `OsgiApplicationProperties`.

### 3. Spring Usage
- Add the Spring submodule to your Spring Boot or Spring application.
- Annotate your configuration class with `@EnableWaterFramework` to auto-configure Water integration.
- Water components and services will be managed by the Spring component registry.
- Use Spring-specific APIs for component registration, filtering, and lifecycle management.
- Application properties are managed via `SpringApplicationProperties`.

### 4. Example: Registering a Component
```java
// OSGi
ComponentRegistry registry = ...;
MyService myService = new MyServiceImpl();
registry.registerComponent(MyService.class, myService, ComponentConfigurationFactory.createNewComponentPropertyFactory().withPriority(2).build());

// Spring
@Autowired
ComponentRegistry registry;
MyService myService = new MyServiceImpl();
registry.registerComponent(MyService.class, myService, ComponentConfigurationFactory.createNewComponentPropertyFactory().withPriority(2).build());
```

### 5. Example: Using Interceptors
- Annotate your service or method with Water's interceptor annotations.
- Interceptors will be applied automatically in both OSGi and Spring environments.

## Properties and Configurations

### Common Properties
- All Water core properties are supported.
- Application properties are loaded via the platform-specific ApplicationProperties implementation.
- OSGi: Properties can be managed via Karaf or OSGi configuration admin.
- Spring: Properties are loaded from application.properties or application.yml.

### OSGi-Specific Properties
- OSGi bundle and service configuration via bnd.bnd and Karaf features.
- Example: `water.testMode=true` (used in tests)

### Spring-Specific Properties
- Spring Boot and Spring application properties.
- Example: `water.testMode=true` (used in tests)

### Test Properties (from test classes)
- Test suites set and validate properties such as `water.testMode`.
- Test configuration classes and property overrides are provided for both OSGi and Spring tests.

## How to Customize Behaviours for This Module

### 1. Custom Component Registry
Implement your own registry by extending `OsgiComponentRegistry` or `SpringComponentRegistry` to add custom registration, filtering, or prioritization logic.

### 2. Custom Interceptors
Add new interceptors by implementing the interceptor interfaces and registering them as components. Use Water's annotation-based AOP to apply them.

### 3. Custom Security Context
Extend `OsgiSecurityContext` or `SpringSecurityContext` to provide custom authentication or authorization logic.

### 4. Custom Application Properties
Implement your own `ApplicationProperties` to load properties from custom sources (e.g., remote config, database).

### 5. Custom Distribution Initializer
Extend `OsgiDistributionInitializer` or `BaseSpringInitializer` to perform custom initialization logic at application startup.

### 6. Custom Filtering
Implement or extend the provided filter builders (`OSGiComponentFilterBuilder`, `SpringComponentFilterBuilder`) to support advanced component selection logic.

### 7. Custom Test Configuration
Override or extend the provided test configuration classes to support custom test scenarios, property overrides, or mock components.

---

The Implementation module is the backbone of Water's cross-platform runtime support, enabling seamless operation, extensibility, and modularity in both OSGi and Spring environments. It is highly customizable and provides robust integration points for advanced use cases.

