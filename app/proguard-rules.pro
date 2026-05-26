-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.shitbox.monitor.**$$serializer { *; }
-keepclassmembers class com.shitbox.monitor.** {
    *** Companion;
}
-keepclasseswithmembers class com.shitbox.monitor.** {
    kotlinx.serialization.KSerializer serializer(...);
}
