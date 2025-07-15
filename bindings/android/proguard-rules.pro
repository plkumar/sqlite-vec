# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep SQLiteVec native methods
-keepclasseswithmembernames class com.sqlite.vec.SQLiteVec {
    native <methods>;
}

# Keep SQLiteVec public API
-keep public class com.sqlite.vec.** {
    public *;
}

# Keep vector serialization methods
-keepclassmembers class com.sqlite.vec.** {
    public static *** serialize*(...);
    public static *** deserialize*(...);
}

# Keep database helper classes
-keep public class * extends com.sqlite.vec.SQLiteVecOpenHelper {
    public <init>(...);
}

# Keep JNI callback methods
-keepclasseswithmembers class * {
    native <methods>;
}
