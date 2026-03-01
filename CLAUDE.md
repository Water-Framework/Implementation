# Implementation Module — Runtime Bridge (OSGi & Spring)

## Purpose
Provides the concrete runtime implementations of the abstractions defined in `Core`. Acts as the bridge between the Water Framework's technology-agnostic API layer and the specific runtime environments: **OSGi (Apache Karaf)** and **Spring Framework**. Every Water application must include exactly one of these implementations at runtime.

## Sub-modules

| Sub-module | Runtime | Key Classes |
|---|---|---|
| `Implementation-osgi` | OSGi / Apache Karaf | `OsgiComponentRegistry`, `OsgiServiceInterceptor`, `OsgiSecurityContext`, `OsgiApplicationProperties`, `WaterBundleActivator` |
| `Implementation-spring` | Spring / Spring Boot | `SpringComponentRegistry`, `SpringServiceInterceptor`, `SpringSecurityContext`, `SpringApplicationProperties`, `@EnableWaterFramework` |

## Implementation-osgi

### Key Components
- **`WaterBundleActivator`** — OSGi `BundleActivator`; bootstraps the framework when the bundle starts
- **`OsgiComponentRegistry`** — Wraps the OSGi `ServiceRegistry`; discovers `@FrameworkComponent` beans via `BundleContext`
- **`OsgiServiceInterceptor`** — Applies the AOP interceptor chain (security, validation, transactions) to OSGi services
- **`OsgiApplicationProperties`** — Reads properties from OSGi `ConfigurationAdmin` or system properties
- **`OsgiSecurityContext`** — Thread-local security context propagation within OSGi bundles

### OSGi-Specific Patterns
```java
// Bundle activation
public class WaterBundleActivator implements BundleActivator {
    @Override
    public void start(BundleContext context) { /* bootstrap */ }
    @Override
    public void stop(BundleContext context)  { /* cleanup  */ }
}
```

### OSGi Deployment
- Requires Apache Karaf 4.4.6+
- Bundles published as OSGi-compatible JARs with `Bundle-SymbolicName` manifest headers
- Feature files in `Distribution-karaf` define installation groups

## Implementation-spring

### Key Components
- **`@EnableWaterFramework`** — Spring `@Import` annotation; enables all Water infrastructure beans
- **`SpringComponentRegistry`** — Backed by Spring `ApplicationContext`; discovers `@FrameworkComponent` beans via `@ComponentScan`
- **`SpringServiceInterceptor`** — Spring AOP `MethodInterceptor`; applies Water's interceptor chain to Spring beans
- **`SpringApplicationProperties`** — Reads from Spring `Environment` (application.properties / YAML / env vars)
- **`SpringSecurityContext`** — Thread-local security context via Spring's `RequestContextHolder`

### Spring Boot Integration
```java
// In your Spring Boot application
@SpringBootApplication
@EnableWaterFramework
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### Spring Test Pattern
```java
@SpringBootTest(classes = MyApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "water.testMode=true",
    "water.rest.security.jwt.validate=false"
})
public class MyRestSpringApiTest {
    @Autowired ComponentRegistry componentRegistry;
    // ...
}
```

## Runtime Selection Rules
- **Do NOT mix** `Implementation-osgi` and `Implementation-spring` in the same JVM
- Spring Boot modules (`-service-spring`) depend on `Implementation-spring`
- OSGi bundles depend on `Implementation-osgi`
- Distribution modules package exactly one runtime per artifact

## Key Principle
Both implementations expose the **same interfaces** from `Core-api`. Application code is runtime-agnostic — only the `Distribution` layer decides which runtime to include.

## Dependencies
- **Common:** `it.water.core:Core-api`, `it.water.core:Core-interceptors`, `it.water.core:Core-registry`
- **OSGi:** `org.osgi:osgi.core`, `org.apache.felix:org.apache.felix.configadmin`, `pax-exam` (testing)
- **Spring:** `org.springframework:spring-context`, `org.springframework:spring-aop`, `org.springframework.boot:spring-boot-autoconfigure`

## Testing
- OSGi: Pax Exam with embedded Karaf container
- Spring: `@SpringBootTest` with `WaterTestExtension` or standard Spring test context
- **Caution:** `BaseSpringInitializer` uses `static boolean initialized` — tests sharing a JVM must reuse the same Spring context config to avoid `ComponentRegistry` missing bean errors
