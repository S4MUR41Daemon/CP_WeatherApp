-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.atmos.weather.**$$serializer { *; }
-keepclassmembers class com.atmos.weather.** { *** Companion; }
-keepclasseswithmembers class com.atmos.weather.** { kotlinx.serialization.KSerializer serializer(...); }

# OkHttp trae sus propias reglas; solo silenciamos avisos opcionales de plataformas TLS.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
