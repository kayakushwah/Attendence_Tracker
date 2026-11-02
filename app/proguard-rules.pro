# ProGuard rules for 75% app

# Vosk
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn org.vosk.**

# Compose
-dontwarn androidx.compose.**

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# General
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
