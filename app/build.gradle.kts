plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dev.rikka.tools.autoresconfig")
    id("net.ankio.xposed") version "1.0.1"
}

android {
    namespace = "net.ankio.bluetooth"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.ankio.bluetooth"
        minSdk = 30
        targetSdk = 33
        versionCode = 9
        versionName = "1.0.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("x86", "armeabi-v7a", "x86_64", "arm64-v8a")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    autoResConfig {
        generateClass.set(true)
        generateRes.set(false)
        generatedClassFullName.set("net.ankio.utils.LangList")
        generatedArrayFirstItem.set("SYSTEM")
    }
    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                "META-INF/ASL2.0",
                "META-INF/gson/FieldAttributes.txt",
                "META-INF/gson/LongSerializationPolicy.txt",
                "META-INF/gson/annotations.txt"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("com.github.AnkioTomas:theme:1.1.5")
    implementation("com.github.AnkioTomas:webdav:1.0.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.github.AnkioTomas.XposedLib:lib:1.0.1")
    compileOnly("de.robv.android.xposed:api:82")
}

xposed {
    entryClass.set("net.ankio.bluetooth.hook.BluetoothXposedEntry")
    moduleDescription.set("A tool that can debug Bluetooth / 一个可以调试蓝牙的工具")
    minXposedVersion.set(93)
    scope("com.android.bluetooth", "net.ankio.bluetooth")
}
