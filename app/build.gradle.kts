plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("org.jetbrains.kotlin.kapt") }
val githubRun=System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?:1
val githubAttempt=System.getenv("GITHUB_RUN_ATTEMPT")?.toIntOrNull()?:1
val automaticVersionCode=githubRun*100+githubAttempt
android {
 namespace="com.aisupercleaner.ultimate"
 compileSdk=36
 defaultConfig { applicationId="com.aisupercleaner.ultimate"; minSdk=26; targetSdk=36; versionCode=automaticVersionCode; versionName="0.1.0"; testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner" }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
 buildFeatures { compose=true }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
 val ks=System.getenv("ANDROID_KEYSTORE_PATH"); val sp=System.getenv("ANDROID_KEYSTORE_PASSWORD"); val ka=System.getenv("ANDROID_KEY_ALIAS"); val kp=System.getenv("ANDROID_KEY_PASSWORD")
 signingConfigs { create("release") { if(ks!=null&&sp!=null&&ka!=null&&kp!=null){storeFile=file(ks);storePassword=sp;keyAlias=ka;keyPassword=kp} } }
 buildTypes { release { isMinifyEnabled=true; isShrinkResources=true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro"); if(ks!=null&&sp!=null&&ka!=null&&kp!=null) signingConfig=signingConfigs.getByName("release") } }
}
dependencies { val bom=platform("androidx.compose:compose-bom:2024.12.01"); implementation(bom); androidTestImplementation(bom); implementation("androidx.core:core-ktx:1.15.0"); implementation("androidx.activity:activity-compose:1.10.0"); implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7"); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.ui:ui-tooling-preview"); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.material:material-icons-extended"); implementation("androidx.room:room-runtime:2.6.1"); implementation("androidx.room:room-ktx:2.6.1"); kapt("androidx.room:room-compiler:2.6.1"); implementation("androidx.media3:media3-transformer:1.5.1"); implementation("androidx.exifinterface:exifinterface:1.3.7"); implementation("com.google.android.gms:play-services-ads:23.6.0"); implementation("com.android.billingclient:billing-ktx:8.0.0"); implementation("com.google.android.ump:user-messaging-platform:4.0.0"); debugImplementation("androidx.compose.ui:ui-tooling"); testImplementation("junit:junit:4.13.2"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0"); androidTestImplementation("androidx.test.ext:junit:1.2.1"); androidTestImplementation("androidx.test:runner:1.6.2"); androidTestImplementation("androidx.test:core:1.6.1"); androidTestImplementation("androidx.compose.ui:ui-test-junit4"); debugImplementation("androidx.compose.ui:ui-test-manifest") }
