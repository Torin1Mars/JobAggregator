import com.android.build.api.dsl.Packaging

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")

    id ("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"

    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.jobaggregator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.jobaggregator"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources.excludes.add("META-INF/*")
        resources.excludes.add("META-INF/DEPENDENCIES/*")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_23
        targetCompatibility = JavaVersion.VERSION_23
    }

    /*kotlinOptions {
        jvmTarget = "17"
    }*/

    buildFeatures {gradle
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    //########################## Project dependencies ##########################

    // Ksoup parser
    val ksoupVer : String = "0.2.1"
    implementation("com.fleeksoft.ksoup:ksoup:$ksoupVer")
    implementation("com.fleeksoft.ksoup:ksoup-network:$ksoupVer")

    //Retrofit
    val retrofitVer : String = "3.0.0"
    implementation ("com.squareup.retrofit2:retrofit:$retrofitVer")
    implementation ("com.squareup.retrofit2:converter-scalars:$retrofitVer")
    implementation ("com.squareup.retrofit2:converter-gson:$retrofitVer")

    //Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    val hiltVersion = "2.57.1"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    testImplementation("androidx.room:room-testing:$room_version")
    ksp("androidx.room:room-compiler:$room_version") // Use your Room version

    implementation ("com.google.code.gson:gson:2.10.1")

    //Selenium
    implementation("org.seleniumhq.selenium:selenium-java:4.20.0")
    implementation("io.github.bonigarcia:webdrivermanager:5.8.0")

}
