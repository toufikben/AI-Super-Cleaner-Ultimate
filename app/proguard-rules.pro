# Keep Room generated implementations and model annotations.
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# Keep Media3 Transformer callbacks and Google SDK callback types.
-keep class androidx.media3.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-keep class com.android.billingclient.** { *; }

-dontwarn javax.annotation.**
