# Keep public DSL and primitives
-keep class runix.dsl.** { *; }
-keep class runix.primitives.** { public *; }

# Strip internal packages
-dontwarn runix.internal.**

# Don't warn on Kotlin internals or stdlib
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Retain source-level info for stacktraces
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable