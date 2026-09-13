-dontwarn androidx.compose.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aisupercleaner.ultimate.**$$serializer { *; }
-keepclassmembers class com.aisupercleaner.ultimate.** { *** Companion; }
-keepclasseswithmembers class com.aisupercleaner.ultimate.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn coil.**
-dontwarn com.patrykandpatrick.vico.**
-keep class com.android.billingclient.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keep class androidx.security.crypto.** { *; }
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
