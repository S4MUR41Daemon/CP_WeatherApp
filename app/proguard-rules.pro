-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.atmos.weather.**$$serializer { *; }
-keepclassmembers class com.atmos.weather.** { *** Companion; }
-keepclasseswithmembers class com.atmos.weather.** { kotlinx.serialization.KSerializer serializer(...); }

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
