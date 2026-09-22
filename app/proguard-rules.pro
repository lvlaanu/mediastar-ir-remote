# kotlinx.serialization ships its own consumer rules, but keeping the generated
# serializers of our @Serializable model classes explicitly is cheap insurance.
-keepclassmembers class com.lvlaanu.mediastarremote.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.lvlaanu.mediastarremote.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ConsumerIrManager is framework API; nothing to keep. No reflection elsewhere.
