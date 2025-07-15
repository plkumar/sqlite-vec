# Consumer proguard rules for SQLiteVec
-keep class com.sqlite.vec.** { *; }
-keepclassmembers class com.sqlite.vec.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
