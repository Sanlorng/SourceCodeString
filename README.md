[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# Source Code String

## What is it?

It is a ksp plugin intended to generate source code string.

## How to use it

1. Add ksp plugin in your module's `build.gradle.kts`

```kotlin
plugins {
   id("com.google.devtools.ksp") version {versionOfKsp}
}
```

2. Add library in your module's `build.gradle.kts`
```kotlin
commonMain {
    dependecies {
        implementation("lib")
    }
}
```
3. Add processor to your module's `build.gradle.kts`
```kotlin
dependencies {
    val processor = project(":source-code-processor")
    add("kspCommonMainMetadata", processor)
}
```
4. Workaround for ksp generate common main source code, add it in your module's `build.gradle.kts`
```kotlin
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
```
5. Add source dir to your `commonMain`
```kotlin
commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
```
6. Add `Sample` annotation to your function, like:
```kotlin
@Sample
fun sampleCode() {
    //Your source code
}
```
when you build the module, the library will generate a string property named `sourceCodeOfSampleCode`, and it's content:
```kotlin
internal val sourceCodeOfSampleCode: String
    get() = """
    |@Sample
    |fun sampleCode() {
    |    //Your source code
    |}
    """.trimMargin()
```

## Advance Usage
See `example` module's `build.gradle.kts`
```kotlin
ksp { 
    // add inline modifier for property getter, default is true
    arg("SourceCodeString.inlineGetter", "false")
    // set property as getter default is true
    arg("SourceCodeString.getter", "false")
    // set package name, default is null, when packageName is not null, the property will generate in a single kt file
    arg("SourceCodeString.packageName", "com.sanlorng.lib.generated")
    // set class name, default is null, when className is not null, will generate a class with the name, and set property as the class' property
    arg("SourceCodeString.className", "Source")
    // generate source class as object, default is true
    arg("SourceCodeString.classAsObject", "false")
    // set property as extendProperty if generated a source class or source object, default is false
    arg("SourceCodeString.extendProperty", "true")
    // set property name template, default is sourceCodeOf%s
    arg("SourceCodeString.nameTemplate", "codeOf%s")
    // set generated file name, default is __SampleCodeString
    arg("SourceCodeString.fileName", "_codeGenerating")
    // transform the function name first char as upper case when generate property name
    arg("SourceCodeString.upperCaseFirstChar", "false")
    // add inline modifier for property
    arg("SourceCodeString.inline", "true")
    // set the custom annotation for tag function, if you don't want to use @Sample
    arg("SourceCodeString.sampleAnnotationName", "SampleCode")
    // set the custom annotation for tag function, if you don't want to use @Sample
    arg("SourceCodeString.sampleAnnotationPackage", "com.sanlorng.lib.other")
}
```
## How do I build it?

1. - [x] Clone this repository ot just [use it as template](https://github.com/Kotlin/multiplatform-library-template/generate)
1. - [ ] Edit library module name and include it in [`settings.gradle.kts`](settings.gradle.kts#L18)
1. - [ ] Edit [`groupId` and `version`](convention-plugins/src/main/kotlin/module.publication.gradle.kts#L10-L11)
    1. If you need the Android support update namespace [there](library/build.gradle.kts#L38) too
    1. If you don't need an Android support delete the [`android` section](library/build.gradle.kts#L37-L43)
1. - [ ] Edit [build targets you need](library/build.gradle.kts#L9-L21)

At this stage, you have everything set to work with Kotlin Multiplatform. The project should be buildable (but you might need to provide actual starting values for the platforms you need).

## How do I make it build on GitHub Actions?

To make it work on GitHub actions, you need to update the [`matrix` section in `gradle.yml`](.github/workflows/gradle.yml#L25-L34). If you didn't change platforms in `build.gradle.kts` you don't need to touch anything. But still read it to understand how it works.

Also, currently, it only runs tests, but you can change this behaviour as you wish by modifying `matrix` and the Gradle [build command](.github/workflows/gradle.yml#L52)

## How do I deploy it to Maven Central?

The most part of the job is already automated for you. However, deployment to Maven Central requires some manual work from your side. 

1. - [ ] Create an account at [Sonatype issue tracker](https://issues.sonatype.org/secure/Signup!default.jspa)
1. - [ ] [Create an issue](https://issues.sonatype.org/secure/CreateIssue.jspa?issuetype=21&pid=10134) to create new project for you
1. - [ ] You will have to prove that you own your desired namespace
1. - [ ] Create a GPG key with `gpg --gen-key`, use the same email address you used to sign up to the Sonatype Jira
1. - [ ] Find your key id in the output of the previous command looking like `D89FAAEB4CECAFD199A2F5E612C6F735F7A9A519`
1. - [ ] Upload your key to a keyserver, for example 
    ```bash
    gpg --send-keys --keyserver keyserver.ubuntu.com "<your key id>"
    ```
1. - [ ] Now you should create secrets available to your GitHub Actions
    1. via `gh` command
    ```bash
    gh secret set OSSRH_GPG_SECRET_KEY -a actions --body "$(gpg --export-secret-key --armor "<your key id>")"
    gh secret set OSSRH_GPG_SECRET_KEY_ID -a actions --body "<your key id>"
    gh secret set OSSRH_GPG_SECRET_KEY_PASSWORD -a actions --body "<your key password>"
    gh secret set OSSRH_PASSWORD -a actions --body "<your sonatype account password>"
    gh secret set OSSRH_USERNAME -a actions --body "<your sonatype account username>"
    ```
    1. Or via the interface in `Settings` → `Secrets and Variables` → `Actions`, same variables as in 1.
1. - [ ] Edit deployment pom parameters in [`module.publication.gradle.kts`](convention-plugins/src/main/kotlin/module.publication.gradle.kts#L25-L44)
1. - [ ] Edit deploy targets in [`deploy.yml`](.github/workflows/deploy.yml#L23-L36)
1. - [ ] Call deployment manually when ready [in Actions](../../actions/workflows/deploy.yml) → `Run Workflow`
1. - [ ] When you see in your account on https://oss.sonatype.org that everything is fine, you can release your staging repositories and add target `releaseSonatypeStagingRepository` to `deploy.yml` [after this line](.github/workflows/deploy.yml#L60). This way artifacts will be published to central automatically when tests pass.
