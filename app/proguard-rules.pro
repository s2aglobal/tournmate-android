# Firebase
-keepattributes Signature
-keepattributes *Annotation*

# Firestore model classes
-keep class com.s2aglobal.tournmate.domain.model.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.s2aglobal.tournmate.**$$serializer { *; }
-keepclassmembers class com.s2aglobal.tournmate.** {
    *** Companion;
}
-keepclasseswithmembers class com.s2aglobal.tournmate.** {
    kotlinx.serialization.KSerializer serializer(...);
}
