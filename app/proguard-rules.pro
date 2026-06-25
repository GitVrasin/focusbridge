# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *

# Kotlin serialization
-keepattributes *Annotation*
-keepclassmembers class kotlinx.** { volatile <fields>; }
