plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.bossfinance"
    compileSdk = 34  // Using SDK 34 for better compatibility

    defaultConfig {
        applicationId = "com.example.bossfinance"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    
    // Enable ViewBinding
    buildFeatures {
        viewBinding = true
    }
    
    // Enhanced packaging options to fix jlink issues
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE*,NOTICE*}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    // Add desugaring support for older Android API levels
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    
    // Explicitly define Kotlin stdlib with the correct version (1.9.22)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Material Components explicitly specified version
    implementation("com.google.android.material:material:1.11.0")
    
    // Add ViewPager2 dependency
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // Chart library for visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Preferences library for settings
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}