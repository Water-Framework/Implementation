---
name: impl-spring-coverage
description: Branch gaps and coverage fixes applied to Implementation-spring; patterns reusable in future coverage work on this module.
metadata:
  type: project
---

## Coverage state after this session (before re-measure)

Baseline: 85% instr / 56% branch / blend 79.7%
Target: ≥80% branch (blend ≥80%)

## Files created / modified

- **NEW**: `src/test/java/it/water/implementation/spring/bundle/WaterPropertiesPropertySourceTest.java`
  — Pure JUnit 5 unit test (no Spring context). 8 @Test methods.
  — Covers all branches of `WaterPropertiesPropertySource.getProperty()`: non-String raw, null raw, no-placeholder String, placeholder+default env absent, placeholder without default, placeholder with empty default, multiple placeholders, mixed literal+placeholder.

- **MODIFIED**: `src/test/java/it/water/implementation/spring/SpringApplicationTest.java`
  — Added 7 new @Test methods and 1 new @Autowired field (SpringApplicationConfiguration).
  — New tests cover: `SpringApplicationProperties.setup()` + `containsKey()`, `SpringApplicationConfiguration.getConfiguration()` + `loadProperties()` already-initialised branch, `SpringComponentRegistry.getApplicationContext()`, `SpringComponentRegistry.unregisterComponent(Class,T)` false-path, `SpringComponentRegistry.findEntitySystemApi/findEntityRepository` null-return branches, priority ordering (null-attribute → -1 default), `WaterSpringConfiguration.getInstance()` non-null branch.

## Key branch-coverage facts for Implementation-spring

- `WaterPropertiesPropertySource` was 0% — pure class, no Spring needed; instantiate directly with `new WaterPropertiesPropertySource("name", props)`.
- `SpringApplicationConfiguration.getConfiguration()` was never called by existing tests; must @Autowired it into the Spring test class.
- `SpringApplicationConfiguration.loadProperties()` null-guard branch (props!=null && copy!=null == true) needed a second call after context startup.
- `SpringComponentRegistry.unregisterComponent(Class,T)` false path: pass a component that was never registered (use `new ServiceInterfaceImpl2()` fresh instance).
- `SpringComponentRegistry.getPriority()` null-attribute path: register components via `ComponentConfigurationFactory`, but existing Spring beans registered via `@FrameworkComponent` don't have the BeanDefinition attribute set (they are registered by the component scanner, not via `registerComponent()`), so the exception-catch branch fires for them. The explicit-priority registration test exercises the happy path (value != null).
- `WaterSpringConfiguration.getInstance()` non-null branch: call `getInstance()` twice and assert `assertSame`.
- `SpringApplicationProperties.containsKey()` and `setup()`: call directly on the @Autowired `ApplicationProperties` bean; the concrete type is `SpringApplicationProperties`.

## Pattern notes

- All Spring integration tests must share the SAME Spring context config (`@ContextConfiguration(classes = TestConfiguration.class)` + `@EnableWaterFramework`) to avoid `ComponentRegistry` missing bean (static `initialized` flag in `BaseSpringInitializer`).
- For pure utility classes (no Spring deps), always prefer a standalone JUnit 5 test with direct instantiation.
- ENV_PATTERN syntax: `${env:VAR_NAME:-default}` — group(1)=varName, group(2)=`:-default`, group(3)=default value. Use env-var names unlikely to exist in CI (e.g. `WATER_TEST_VAR_UNLIKELY_XYZ_12345678`). Guard assertions with `if (System.getenv(varName) == null)` to stay non-flaky.
