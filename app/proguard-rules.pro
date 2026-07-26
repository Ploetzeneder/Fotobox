-keep class com.fotobox.app.data.models.** { *; }
-keep class androidx.camera.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
