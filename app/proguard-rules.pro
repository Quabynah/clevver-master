-dontobfuscate #STOPSHIP remove this
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
# Retain service method parameters.
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn io.sentry.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-keep class io.clevver.data.api.dribbble.model.**  { *; }
-keep class io.clevver.data.api.designernews.model.**  { *; }
-keep class io.clevver.data.api.producthunt.model.**  { *; }
-keep class io.clevver.ui.transitions.**  { *; }
-keep class android.support.v7.widget.LinearLayoutManager {
    public protected *;
}
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep class in.uncod.android.bypass.** { *; }

-keepattributes *Annotation*,Signature,Exceptions

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# This optimization conflicts with how Retrofit uses proxy objects without concrete implementations
-optimizations !method/removal/parameter
