# Allure TestOps Test Case Sync — Gradle Plugin

A Gradle plugin that scans your test sources for `@Test` methods missing `@AllureId` annotations, automatically creates corresponding test cases in [Allure TestOps](https://qameta.io/), and writes the `@AllureId("N")` annotation back into the source code.

## Features

- Scans Kotlin and Java test sources (`src/test/kotlin`, `src/test/java`) across all subprojects
- Detects `@Test` methods (JUnit 5) that do not have an `@AllureId` annotation
- Creates test cases in Allure TestOps via REST API
- Uses `@DisplayName` value as the test case name when present, falls back to the method name
- Inserts `@AllureId("N")` annotation and the required import automatically
- Supports Kotlin backtick-quoted method names

## Requirements

- Gradle 9.x
- JDK 21+
- An Allure TestOps instance with API access

## Installation

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.nuke228.allure-sync-testcases") version "1.0.0"
}
```

If using a multi-module project, apply the plugin to the root project. It will scan test sources in all subprojects.

## Configuration

```kotlin
allureTestOps {
    url.set("https://your-allure-instance.com")
    token.set(providers.environmentVariable("ALLURE_TOKEN"))
    projectId.set(42L)
}
```

| Property    | Description                                      |
|-------------|--------------------------------------------------|
| `url`       | Base URL of your Allure TestOps instance          |
| `token`     | API token (recommended: pass via environment var) |
| `projectId` | Allure TestOps project ID                         |

### Obtaining an API Token

1. Log in to your Allure TestOps instance
2. Go to your profile settings
3. Generate an API token

## Usage

```bash
ALLURE_TOKEN=your-api-token ./gradlew syncAllureTestCases
```

### Example

Before:
```kotlin
@Test
@DisplayName("User can log in with valid credentials")
fun `user can log in with valid credentials`() { ... }
```

After running `syncAllureTestCases`:
```kotlin
@AllureId("12345")
@Test
@DisplayName("User can log in with valid credentials")
fun `user can log in with valid credentials`() { ... }
```

The test case name in Allure TestOps will be `"User can log in with valid credentials"` (from `@DisplayName`).

## How It Works

1. Authenticates with Allure TestOps using the provided API token (OAuth token exchange)
2. Scans `src/test/kotlin` and `src/test/java` directories in the root project and all subprojects
3. For each `@Test` method without `@AllureId`:
   - Creates a test case in Allure TestOps via `POST /api/rs/testcase`
   - Inserts `@AllureId("N")` annotation before the `@Test` annotation in the source file
   - Adds `import io.qameta.allure.AllureId` if not already present

## License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.
