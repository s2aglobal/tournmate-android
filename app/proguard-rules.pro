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

# Credential Manager: the Play Services provider is loaded reflectively.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Strip verbose/debug/info logs from release builds (warnings and errors stay).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
