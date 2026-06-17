plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val composeDebugMainJar = project(":compose").layout.buildDirectory.file(
    "intermediates/aar_main_jar/debug/syncDebugLibJars/classes.jar"
)

android {
    namespace = "com.kulipai.luacompose"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kulipai.luacompose"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0-experimental"

//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.jvmArgs("-Xverify:none")
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)




    implementation(project(":androlua"))

    implementation(project(":compose"))

    testImplementation(kotlin("reflect"))
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly(files(composeDebugMainJar))
}

tasks.matching { it.name == "testDebugUnitTest" }.configureEach {
    dependsOn(":compose:syncDebugLibJars")
}
