import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        // Shitpack repo which contains our tools and dependencies
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        // Cloudstream gradle plugin which makes everything work and builds plugins
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) = extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    android {
        namespace = "com.example"

        compileSdkVersion(36)

        defaultConfig {
            minSdk = 21
            targetSdk = 36
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    tasks.withType<KotlinJvmCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            // Disables some unnecessary kotlin checks
            freeCompilerArgs.addAll(
                "-Xno-call-assertions",
                "-Xno-param-assertions",
                "-Xno-receiver-assertions"
            )
        }
    }

    afterEvaluate {
        dependencies {
            val implementation by configurations
            val apk by configurations

            // Stubs for all Cloudstream classes
            apk("com.lagradost:cloudstream3:pre-release")

            // these dependencies can include any of those which are dependencies of cloudstream3
            // so they don't need to be included in the apk
            // see https://github.com/recloudstream/cloudstream/blob/master/app/build.gradle.kts
            implementation(kotlin("stdlib")) // adds standard library
            implementation("com.github.Blatzar:NiceHttp:0.4.11") // http library
            implementation("org.jsoup:jsoup:1.18.3") // html parser
            implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
            implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
        }
    }

    cloudstream {
        // ---- REPLACE WITH YOUR INFO ----
        // CloudStream will use this url to update plugins.
        // Format: raw.githubusercontent.com/{YOUR_GITHUB_USERNAME}/{REPO_NAME}/{builds_branch}
        // Example: raw.githubusercontent.com/YourUsername/TestPlugins/builds
        setRepo("raw.githubusercontent.com/Rodro-eng/cdProject/builds")
    }
}
