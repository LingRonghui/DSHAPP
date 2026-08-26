# Add project specific ProGuard rules here.
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.dsh.harness.**$$serializer { *; }
-keepclassmembers class com.dsh.harness.** {
    *** Companion;
}
-keepclasseswithmembers class com.dsh.harness.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
