plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    `maven-publish`
}

android {
    namespace = "com.kulipai.luacompose.compose"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
    
    sourceSets {
        getByName("main") {
            java.srcDir("build/generated/ksp/debug/kotlin")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)

    implementation(project(":annotations"))
     ksp(project(":compiler"))
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlin.reflect) // added kotlin-reflect
    debugImplementation(libs.androidx.compose.ui.tooling)


    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)


    implementation(project(":androlua"))

}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.kulipai.luacompose"
                artifactId = "compose"
                version = "1.0.0-SNAPSHOT"
            }
        }
    }
}
