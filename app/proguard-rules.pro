# Keep Room entities and model annotations used by generated database code.
-keep @androidx.room.Entity class * { *; }

# Keep the application's Room database contract and generated implementation.
-keep class com.aisupercleaner.ultimate.data.AppDatabase { *; }
-keep class com.aisupercleaner.ultimate.data.AppDatabase_Impl { *; }

-dontwarn javax.annotation.**
