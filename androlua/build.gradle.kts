plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.kulipai.luahook.androlua"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}


dependencies {
    implementation(libs.androidx.core.ktx)

    api(fileTree("libs"))

}