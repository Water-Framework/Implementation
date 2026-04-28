# Implementation Module — Runtime Platform Integration (OSGi & Spring)

## Purpose
Provides the runtime-specific implementations of Water Framework core abstractions. Bridges the gap between Water's platform-agnostic interfaces (in `Core`) and the actual runtime environment (Apache Karaf/OSGi or Spring Boot). Contains `ComponentRegistry`, `SecurityContext`, `ApplicationProperties`, and AOP interceptors for each platform. Does NOT contain business logic — all module implementations depend on Core interfaces, not on this module.

## Sub-modules

| Sub-module | Runtime | Key Classes |
|---|---|---|
| `Implementation-osgi` | Water/OSGi (Apache Karaf) | `OsgiComponentRegistry`, `WaterBundleActivator`, `OsgiServiceInterceptor`, `OsgiApplicationProperties`, `OsgiSecurityContext`, `OsgiDistributionInitializer` |
| `Implementation-spring` | Spring Boot | `SpringComponentRegistry`, `WaterSpringConfiguration`, `SpringServiceInterceptor`, `SpringApplicationProperties`, `SpringSecurityContext`, `BaseSpringInitializer` |

## OSGi Implementation

### OsgiComponentRegistry
The BundleContext-backed implementation of `ComponentRegistry`:

```java
@FrameworkComponent
public class OsgiComponentRegistry extends AbstractComponentRegistry {
    // Queries OSGi service registry for components
    @Override
    public <T> List<T> findComponents(Class<T> componentClass, ComponentFilter filter) { ... }

    // Registers a Water service as an OSGi service, wrapping it in a proxy
    @Override
    public <T, K> ComponentRegistration<T, K> registerComponent(
        Class<? extends T> componentClass, T component, ComponentConfiguration configuration) { ... }
}
```

Key behaviors:
- Components are registered as OSGi services via `BundleContext.registerService()`
- Each registered Water service is **automatically wrapped in a dynamic proxy** (`OsgiServiceInterceptor`) that applies security, logging, and validation
- Priority-based selection: higher priority components are returned first by `findComponent()`

### WaterBundleActivator
OSGi bundle entry point — starts and stops the Water framework:

```java
public class WaterBundleActivator implements BundleActivator {
    @Override public void start(BundleContext context) { /* init ComponentRegistry, start framework */ }
    @Override public void stop(BundleContext context)  { /* invoke @OnDeactivate, unregister services */ }
}
```

### OsgiServiceInterceptor
Dynamic proxy that wraps every registered Water service:

```java
// Applied automatically — intercepts every method call on a @FrameworkComponent
// Handles: @AllowPermissions, @AllowRoles, @AllowPermissionsOnReturn
// Security check order: extract SecurityContext → check roles/permissions → proceed or throw UnauthorizedException
```

### OsgiApplicationProperties
Reads configuration from Karaf's `etc/` directory:

```java
// Property resolution order:
// 1. System properties (-D flags)
// 2. Karaf cfg files (etc/it.water.*.cfg)
// 3. Classpath defaults
```

## Spring Implementation

### @EnableWaterFramework
Spring Boot activation annotation:

```java
@SpringBootApplication
@EnableWaterFramework      // Activates Water DI, component scanning, AOP
public class MyApplication { ... }
```

### SpringComponentRegistry
Spring ApplicationContext-backed implementation:

```java
@Configuration
public class WaterSpringConfiguration {
    @Bean
    public SpringComponentRegistry componentRegistry(ApplicationContext context) { ... }
}
```

### SpringServiceInterceptor
Spring AOP aspect wrapping all `@FrameworkComponent` beans:

```java
@Aspect @Component
public class SpringServiceInterceptor {
    @Around("@within(it.water.core.api.registry.FrameworkComponent)")
    public Object intercept(ProceedingJoinPoint pjp) throws Throwable {
        // Apply @AllowPermissions, @AllowRoles checks before proceeding
    }
}
```

## Component Lifecycle (Both Platforms)

```
1. Discovery    — @FrameworkComponent scanning (compile-time via Atteo ClassIndex)
2. Instantiation — Container creates bean/service instance
3. DI           — @Inject fields resolved from ComponentRegistry
4. Registration — registerComponent() → proxied, added to registry
5. Activation   — @OnActivate method invoked (post-DI)
6. Service Ready — accessible via findComponent()
7. Deactivation — @OnDeactivate invoked (on shutdown/uninstall)
8. Unregistration — removed from registry
```

## Proxy Registration Flow (OSGi)

```
@FrameworkComponent service registered
  └─► OsgiComponentRegistry.registerComponent()
       └─► Creates dynamic proxy via OsgiServiceInterceptor
            └─► Proxy registered as OSGi service
                 └─► Consumers receive the proxy (not the original bean)
                      └─► Every method call passes through interceptor chain
```

## Dependencies

### Implementation-osgi
- `it.water.core:Core-api` — all framework interfaces
- `org.osgi:osgi.core` — `BundleContext`, `ServiceReference`, etc.
- `org.apache.karaf` — Karaf-specific runtime
- `org.ops4j.pax.exam` — OSGi integration testing

### Implementation-spring
- `it.water.core:Core-api` — all framework interfaces
- `org.springframework:spring-context` — ApplicationContext
- `org.springframework:spring-aop` — AOP for interceptors
- `org.springframework.boot:spring-boot-autoconfigure` — Auto-configuration

## Testing
- OSGi tests: use Pax Exam with Karaf feature provisioning (from `*-features` modules)
- Spring tests: use `@SpringBootTest` with `@EnableWaterFramework`
- Unit tests: **never** depend on this module directly — test via Core interfaces
- REST tests: **Karate only** for any HTTP endpoint testing

## Code Generation Rules
- Application modules should NEVER import from `Implementation-osgi` or `Implementation-spring` directly
- All module code must depend only on `Core-*` interfaces to remain runtime-agnostic
- `@FrameworkComponent` (from Core) is the ONLY annotation needed to register a service — the runtime picks it up automatically
- When writing tests, use `WaterTestExtension` (from `Core-testing-utils`) — it creates an in-memory registry without a real OSGi/Spring runtime
- OSGi-specific: BND plugin generates `MANIFEST.MF` automatically from `bnd.bnd` files — never edit the manifest manually
- Spring-specific: add `@EnableWaterFramework` to your `@SpringBootApplication` class to activate the entire Water runtime
