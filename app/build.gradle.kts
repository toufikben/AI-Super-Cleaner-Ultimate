plugins {


    id("com.android.application")


    id("org.jetbrains.kotlin.android")


    id("org.jetbrains.kotlin.plugin.compose")


    id("org.jetbrains.kotlin.kapt")


}
val githubRun = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val githubAttempt = System.getenv("GITHUB_RUN_ATTEMPT")?.toIntOrNull() ?: 1
val automaticVersionCode = (githubRun * 100) + githubAttempt

android {


    namespace = "com.aisupercleaner.ultimate"


    compileSdk = 36






    defaultConfig {


        applicationId = "com.aisupercleaner.ultimate"


        minSdk = 26


        targetSdk = 36


        versionCode = automaticVersionCode


        versionName = "0.1.0"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


    }






    compileOptions {


        sourceCompatibility = JavaVersion.VERSION_17


        targetCompatibility = JavaVersion.VERSION_17


    }






    kotlinOptions { jvmTarget = "17" }






    buildFeatures { compose = true }


    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }






    val releaseKeystore = System.getenv("ANDROID_KEYSTORE_PATH")


    val releaseStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")


    val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")


    val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")


    signingConfigs {


        create("release") {


            if (releaseKeystore != null && releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null) {


                storeFile = file(releaseKeystore)


                storePassword = releaseStorePassword


                keyAlias = releaseKeyAlias


                keyPassword = releaseKeyPassword


            }


        }


    }


    buildTypes {


        release {


            isMinifyEnabled = true
