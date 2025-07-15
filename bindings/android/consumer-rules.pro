# Keep consumer proguard rules minimal for library users
# The main library proguard rules are in proguard-rules.pro

# Keep public API classes and methods for library consumers
-keep public class com.sqlite.vec.** {
    public *;
}
